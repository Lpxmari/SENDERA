package com.sendera.repository;

import com.sendera.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** NC-01: los datos del perfil del viajero se guardan en la base (H2) y se leen igual, en el mismo orden. */
@DataJpaTest
@ActiveProfiles("test")
class UsuarioPerfilIntegracionTest {

    @Autowired TestEntityManager em;
    @Autowired UsuarioRepository usuarioRepository;

    private Long crearUsuario() {
        Usuario usuario = Usuario.builder().nombre("María").apellidos("Gutiérrez").correo("maria@correo.com")
                .contrasenaHash("hash").rol("ROLE_CLIENTE").numeroIdentificacion("1094123456")
                .fechaNacimiento(LocalDate.of(1958, 3, 23)).paisResidencia("Colombia").build();
        usuario.getDestinosFavoritos().addAll(List.of("Salento", "Cartagena", "Villa de Leyva"));
        usuario.getIntereses().addAll(List.of("Naturaleza", "Gastronomía"));
        Long id = em.persistAndFlush(usuario).getId();
        em.clear();
        return id;
    }

    @Test
    void guardaYLeeLosDatosDelPerfilConservandoElOrden() {
        Long id = crearUsuario();

        Usuario leido = usuarioRepository.findByCorreo("maria@correo.com").orElseThrow();

        assertEquals(id, leido.getId());
        assertEquals("1094123456", leido.getNumeroIdentificacion());
        assertEquals(LocalDate.of(1958, 3, 23), leido.getFechaNacimiento());
        assertEquals("Colombia", leido.getPaisResidencia());
        assertEquals(List.of("Salento", "Cartagena", "Villa de Leyva"), leido.getDestinosFavoritos());
        assertEquals(List.of("Naturaleza", "Gastronomía"), leido.getIntereses());
    }

    @Test
    void reemplazarLasListasActualizaLaBase() {
        Long id = crearUsuario();
        Usuario usuario = usuarioRepository.findById(id).orElseThrow();
        usuario.getDestinosFavoritos().clear();
        usuario.getDestinosFavoritos().addAll(List.of("Cartagena", "Salento"));
        usuario.getIntereses().clear();
        em.flush();
        em.clear();

        Usuario leido = usuarioRepository.findById(id).orElseThrow();

        assertEquals(List.of("Cartagena", "Salento"), leido.getDestinosFavoritos());
        assertEquals(List.of(), leido.getIntereses());
    }

    @Test
    void unaCuentaSinPerfilTieneCamposVacios() {
        Long id = em.persistAndFlush(Usuario.builder().nombre("Luis").apellidos("Pérez").correo("luis@correo.com")
                .contrasenaHash("hash").rol("ROLE_CLIENTE").build()).getId();
        em.clear();

        Usuario leido = usuarioRepository.findById(id).orElseThrow();

        assertNull(leido.getNumeroIdentificacion());
        assertNull(leido.getFechaNacimiento());
        assertEquals(List.of(), leido.getDestinosFavoritos());
        assertEquals(List.of(), leido.getIntereses());
    }
}
