package de.seuhd.campuscoffee.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain

/**
 * Spring Security configuration.
 *
 * The starter ships a deliberately *permissive* chain so every endpoint stays open and the existing
 * open-endpoint tests keep passing: students tighten this into a real chain rather than starting from a
 * blank slate. The supporting beans (password encoder, authentication provider/manager, JSON 401 entry
 * point) and the JWT resource-server wiring are already in place so the authentication and JWT exercises
 * are about *policy*, not plumbing.
 */
@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        authenticationEntryPoint: AuthenticationEntryPoint
    ): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                // POS auslesen
                authorize(HttpMethod.GET, "/api/pos", permitAll)
                authorize(HttpMethod.GET, "/api/pos/*", permitAll)

                // Review auslesen
                authorize(HttpMethod.GET, "/api/reviews", permitAll)
                authorize(HttpMethod.GET, "/api/reviews/*", permitAll)

                //  Nutzer nach ID auslesen
                authorize(HttpMethod.GET, "/api/users/filter", authenticated)
                authorize(HttpMethod.GET, "/api/users/{id}", authenticated)

                // Alle User auslesen
                authorize(HttpMethod.GET, "/api/users", hasRole("ADMIN"))

                // Nutzer registrieren
                authorize(HttpMethod.POST, "/api/users", permitAll)

                //  Nutzer nach ID bearbeiten
                authorize(HttpMethod.PUT, "/api/users/{id}", authenticated)

                // Nutzer löschen
                authorize(HttpMethod.DELETE, "/api/users/{id}", hasRole("ADMIN"))

                // Review posten
                authorize(HttpMethod.POST, "/api/reviews", authenticated)

                // Review bearbeiten
                authorize(HttpMethod.PUT, "/api/reviews/{id}", authenticated)

                // Review löschen
                authorize(HttpMethod.DELETE, "/api/reviews/{id}", authenticated)

                // Review "approven"
                authorize(HttpMethod.PUT, "/api/reviews/{id}/approve", authenticated)

                // POS bearbeiten
                authorize(HttpMethod.POST, "/api/pos", hasRole("MODERATOR"))
                authorize(HttpMethod.POST, "/api/pos/import/osm/{nodeId}", hasRole("MODERATOR"))
                authorize(HttpMethod.PUT, "/api/pos/{id}", hasRole("MODERATOR"))
                authorize(HttpMethod.DELETE, "/api/pos/{id}", hasRole("MODERATOR"))

                // Swagger UI Zugriff
                authorize(HttpMethod.GET, "/api/swagger-ui/**", permitAll)

                // Documentation Zugriff
                authorize(HttpMethod.GET, "/api/api-docs/**", permitAll)

                // Dev Zugriff
                authorize("/api/dev/**", permitAll)

                // TODO (Exercise 3): curating a POS (POST/PUT/DELETE `/pos`) requires the `MODERATOR` role,
                //  and deleting a user (DELETE `/users/{id}`) requires `ADMIN`; add these rules before the
                //  catch-all so they take precedence.
                authorize(anyRequest, authenticated)
            }
            // Stateless API: no server-side session; the principal comes from the credentials on each request.
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            // Accept HTTP Basic credentials. Bearer-token (JWT) support is wired below.
            httpBasic { }
            // Bearer-token (JWT) resource server. Harmless under permitAll: a missing or invalid token
            // leaves the request anonymous, which permitAll still allows.
            // TODO (Exercise 4): add a JwtAuthenticationConverter that maps the token's `roles` claim to
            //  `ROLE_*` authorities, so a Bearer principal carries the same authorities as a Basic one.
            oauth2ResourceServer { jwt { } }
            // Render an unauthenticated rejection as the application's JSON ErrorResponse (takes effect
            // once the chain requires authentication).
            exceptionHandling { this.authenticationEntryPoint = authenticationEntryPoint }
        }
        return http.build()
    }

    /** Delegating encoder ({bcrypt} by default); shared with the data layer's hashing semantics. */
    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    /** Authenticates username/password against the [UserDetailsService] using the shared encoder. */
    @Bean
    fun authenticationProvider(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): DaoAuthenticationProvider {
        val provider = DaoAuthenticationProvider(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)
        return provider
    }

    /** Exposes the [AuthenticationManager] so the token endpoint (Exercise 4) can reuse it. */
    @Bean
    fun authenticationManager(authenticationProvider: DaoAuthenticationProvider): AuthenticationManager =
        AuthenticationManager { authentication -> authenticationProvider.authenticate(authentication) }
}
