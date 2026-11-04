package com.sendera.service;

import com.sendera.dto.CrearResenaDto;
import com.sendera.dto.ResenaDto;
import com.sendera.exception.ArchivoImagenInvalidoException;
import com.sendera.exception.ResenaDuplicadaException;
import com.sendera.exception.ResenaNoPermitidaException;
import com.sendera.model.Destino;
import com.sendera.model.EstadoReserva;
import com.sendera.model.Resena;
import com.sendera.model.Usuario;
import com.sendera.repository.DestinoRepository;
import com.sendera.repository.ResenaRepository;
import com.sendera.repository.ReservaRepository;
import com.sendera.repository.UsuarioRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.ResenaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResenaServiceImplTest {

    private static final UsuarioActual CLIENTE = new UsuarioActual(8L, false);
    private static final UsuarioActual ADMIN = new UsuarioActual(1L, true);

    @Mock
    ResenaRepository resenaRepository;
    @Mock
    DestinoRepository destinoRepository;
    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    ReservaRepository reservaRepository;
    @InjectMocks
    ResenaServiceImpl resenaService;

    private Destino destino;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        destino = Destino.builder().id(3L).build();
        usuario = Usuario.builder().id(8L).nombre("Ana").apellidos("Pérez").build();
    }

    private void destinoYUsuarioExisten() {
        when(destinoRepository.existsById(3L)).thenReturn(true);
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(usuario));
    }

    private void conReservaConfirmada(boolean confirmada) {
        when(reservaRepository.existsByUsuarioIdAndItinerarioDestinoIdAndEstadoReserva(
                8L, 3L, EstadoReserva.CONFIRMADA)).thenReturn(confirmada);
    }

    private void conResenaPrevia(boolean existe) {
        when(resenaRepository.existsByUsuarioIdAndDestinoId(8L, 3L)).thenReturn(existe);
    }

    private void guardadoAsigna() {
        when(destinoRepository.getReferenceById(3L)).thenReturn(destino);
        when(resenaRepository.saveAndFlush(any(Resena.class))).thenAnswer(invocacion -> {
            Resena resena = invocacion.getArgument(0);
            resena.setId(11L);
            resena.setCreadaEn(LocalDateTime.now());
            return resena;
        });
    }

    @Test
    void crearAsignaUsuarioDelTokenYGuardaImagenBinaria() {
        destinoYUsuarioExisten();
        conReservaConfirmada(true);
        conResenaPrevia(false);
        guardadoAsigna();

        byte[] imagen = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
        MockMultipartFile archivo = new MockMultipartFile("imagen", "foto.png", "image/png", imagen);
        CrearResenaDto dto = crearDto("Hermoso lugar", 5);

        ResenaDto resultado = resenaService.crear(3L, dto, archivo, CLIENTE);

        ArgumentCaptor<Resena> resenaGuardada = ArgumentCaptor.forClass(Resena.class);
        verify(resenaRepository).saveAndFlush(resenaGuardada.capture());
        assertEquals(usuario, resenaGuardada.getValue().getUsuario());
        assertEquals(destino, resenaGuardada.getValue().getDestino());
        assertArrayEquals(imagen, resenaGuardada.getValue().getImagen());
        assertEquals("image/png", resenaGuardada.getValue().getImagenContentType());
        assertEquals("Ana Pérez", resultado.getAutor());
        assertEquals(5, resultado.getCalificacion());
        assertEquals("/destinos/3/resenas/11/imagen", resultado.getImagenUrl());
    }

    @Test
    void crearSinImagenNoIncluyeRutaDeImagen() {
        destinoYUsuarioExisten();
        conReservaConfirmada(true);
        conResenaPrevia(false);
        guardadoAsigna();

        ResenaDto resultado = resenaService.crear(3L, crearDto("Un buen viaje", 4), null, CLIENTE);

        assertNull(resultado.getImagenUrl());
        verify(resenaRepository).saveAndFlush(any(Resena.class));
    }

    @Test
    void sinReservaConfirmadaDelDestinoLanzaProhibidoYNoGuarda() {
        // Cubre reserva ausente, pendiente, cancelada o de otro destino: la consulta solo cuenta CONFIRMADA del destino.
        destinoYUsuarioExisten();
        conReservaConfirmada(false);

        ResenaNoPermitidaException error = assertThrows(ResenaNoPermitidaException.class,
                () -> resenaService.crear(3L, crearDto("Comentario", 3), null, CLIENTE));

        assertEquals("Solo puedes opinar sobre destinos que ya reservaste y cuya reserva fue confirmada.",
                error.getMessage());
        verify(resenaRepository, never()).saveAndFlush(any(Resena.class));
    }

    @Test
    void elAdministradorSinReservaConfirmadaTambienRecibeProhibido() {
        when(destinoRepository.existsById(3L)).thenReturn(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(Usuario.builder().id(1L).nombre("Admin").apellidos("S").build()));
        when(reservaRepository.existsByUsuarioIdAndItinerarioDestinoIdAndEstadoReserva(
                1L, 3L, EstadoReserva.CONFIRMADA)).thenReturn(false);

        assertThrows(ResenaNoPermitidaException.class,
                () -> resenaService.crear(3L, crearDto("Comentario", 3), null, ADMIN));
        verify(resenaRepository, never()).saveAndFlush(any(Resena.class));
    }

    @Test
    void laConsultaDeReservaUsaElUsuarioDelTokenYSoloElEstadoConfirmada() {
        destinoYUsuarioExisten();
        conReservaConfirmada(false);

        assertThrows(ResenaNoPermitidaException.class,
                () -> resenaService.crear(3L, crearDto("Comentario", 3), null, CLIENTE));

        verify(reservaRepository).existsByUsuarioIdAndItinerarioDestinoIdAndEstadoReserva(
                8L, 3L, EstadoReserva.CONFIRMADA);
        verify(reservaRepository, never()).existsByUsuarioIdAndItinerarioDestinoIdAndEstadoReserva(
                8L, 3L, EstadoReserva.PENDIENTE);
    }

    @Test
    void conReseñaPreviaDelMismoDestinoLanzaConflictoYNoGuarda() {
        destinoYUsuarioExisten();
        conReservaConfirmada(true);
        conResenaPrevia(true);

        ResenaDuplicadaException error = assertThrows(ResenaDuplicadaException.class,
                () -> resenaService.crear(3L, crearDto("Otra vez", 5), null, CLIENTE));

        assertEquals("Ya publicaste una reseña de este destino.", error.getMessage());
        verify(resenaRepository, never()).saveAndFlush(any(Resena.class));
    }

    @Test
    void siLaBaseRechazaPorRestriccionUnicaTambienLanzaConflicto() {
        // Dos envíos simultáneos pasan la verificación; el segundo choca con UNIQUE (usuario_id, destino_id).
        destinoYUsuarioExisten();
        conReservaConfirmada(true);
        conResenaPrevia(false);
        when(destinoRepository.getReferenceById(3L)).thenReturn(destino);
        when(resenaRepository.saveAndFlush(any(Resena.class))).thenThrow(new DataIntegrityViolationException("uq"));

        assertThrows(ResenaDuplicadaException.class,
                () -> resenaService.crear(3L, crearDto("Comentario", 3), null, CLIENTE));
    }

    @Test
    void rechazaArchivoQueNoEsUnaImagenDeclarada() {
        destinoYUsuarioExisten();
        conReservaConfirmada(true);
        conResenaPrevia(false);
        MockMultipartFile archivo = new MockMultipartFile(
                "imagen", "texto.png", "image/png", "no es una imagen".getBytes());

        assertThrows(ArchivoImagenInvalidoException.class,
                () -> resenaService.crear(3L, crearDto("Comentario", 3), archivo, CLIENTE));
        verify(resenaRepository, never()).saveAndFlush(any(Resena.class));
    }

    @Test
    void siFallaUnaReglaNoSeProcesaLaImagen() {
        // Con una imagen inválida pero sin reserva confirmada, el error es el de permisos (403), no el de la imagen.
        destinoYUsuarioExisten();
        conReservaConfirmada(false);
        MockMultipartFile archivo = new MockMultipartFile(
                "imagen", "texto.png", "image/png", "no es una imagen".getBytes());

        assertThrows(ResenaNoPermitidaException.class,
                () -> resenaService.crear(3L, crearDto("Comentario", 3), archivo, CLIENTE));
    }

    private CrearResenaDto crearDto(String comentario, int calificacion) {
        CrearResenaDto dto = new CrearResenaDto();
        dto.setComentario(comentario);
        dto.setCalificacion(calificacion);
        return dto;
    }
}
