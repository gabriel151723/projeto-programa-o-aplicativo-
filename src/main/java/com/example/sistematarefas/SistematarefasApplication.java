package com.example.sistematarefas;

import jakarta.persistence.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SpringBootApplication
public class SistematarefasApplication {
    public static void main(String[] args) {
        SpringApplication.run(SistematarefasApplication.class, args);
    }
}

@Entity
@Table(name = "usuarios")
class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String nome;
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    @Column(nullable = false, length = 255)
    private String senha;
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}

@Entity
@Table(name = "tarefas")
class Tarefa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String titulo;
    @Column(columnDefinition = "TEXT")
    private String descricao;
    @Column(nullable = false, length = 50)
    private String status = "pendente";
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
}

interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    List<Tarefa> findAllByOrderByCriadoEmDesc();
}

@Service
class UsuarioService implements UserDetailsService {
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    public Usuario cadastrar(Usuario usuario) {
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Não encontrado: " + email));
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenha())
                .roles("USER")
                .build();
    }
}

@Service
class TarefaService {
    @Autowired private TarefaRepository tarefaRepository;
    public List<Tarefa> listar() { return tarefaRepository.findAllByOrderByCriadoEmDesc(); }
    public Tarefa criar(Tarefa t) { return tarefaRepository.save(t); }
    public void excluir(Long id) { tarefaRepository.deleteById(id); }
}

@RestController
@RequestMapping("/api/usuarios")
class UsuarioApiController {
    @Autowired private UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody Usuario usuario) {
        Usuario criado = usuarioService.cadastrar(usuario);
        return ResponseEntity.status(201).body(Map.of("mensagem", "Usuário cadastrado!", "id", criado.getId()));
    }
}

@RestController
@RequestMapping("/api/tarefas")
class TarefaApiController {
    @Autowired private TarefaService tarefaService;

    @GetMapping
    public List<Tarefa> listar() { return tarefaService.listar(); }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Tarefa tarefa) {
        Tarefa criada = tarefaService.criar(tarefa);
        return ResponseEntity.status(201).body(Map.of("mensagem", "Tarefa criada!", "id", criada.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        tarefaService.excluir(id);
        return ResponseEntity.ok(Map.of("mensagem", "Tarefa excluída!"));
    }
}

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/usuarios", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }
}
