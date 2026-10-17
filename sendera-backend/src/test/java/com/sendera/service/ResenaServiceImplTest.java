package com.sendera.service;

import com.sendera.dto.CrearResenaDto;
import com.sendera.dto.ResenaDto;
import com.sendera.exception.ArchivoImagenInvalidoException;
import com.sendera.model.Destino;
import com.sendera.model.Resena;
import com.sendera.model.Usuario;
import com.sendera.repository.DestinoRepository;
import com.sendera.repository.ResenaRepository;
import com.sendera.repository.UsuarioRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.ResenaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @Mock
    ResenaRepository resenaRepository;
    @Mock
    DestinoRepository destinoRepository;
    @Mock
    UsuarioRepository usuarioRepository;
    @InjectMocks
    ResenaServiceImpl resenaService;

    @Test
    void crearAsignaUsuarioDelTokenYGuardaImagenBinaria() {
        Destino destino = Destino.builder().id(3L).build();
        Usuario usuario = Usuario.builder().id(8L).nombre("Ana").apellidos("Pérez").build();
        when(destinoRepository.existsById(3L)).thenReturn(true);
        when(destinoRepository.getReferenceById(3L)).thenReturn(destino);
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(usuario));
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocacion -> {
            Resena resena = invocacion.getArgument(0);
            resena.setId(11L);
            resena.setCreadaEn(LocalDateTime.now());
            return resena;
        });

        byte[] imagen = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
        MockMultipartFile archivo = new MockMultipartFile("imagen", "foto.png", "image/png", imagen);
        CrearResenaDto dto = crearDto("Hermoso lugar", 5);

        ResenaDto resultado = resenaService.crear(3L, dto, archivo, CLIENTE);

        ArgumentCaptor<Resena> resenaGuardada = ArgumentCaptor.forClass(Resena.class);
        verify(resenaRepository).save(resenaGuardada.capture());
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
        Destino destino = Destino.builder().id(3L).build();
        Usuario usuario = Usuario.builder().id(8L).nombre("Ana").apellidos("Pérez").build();
        when(destinoRepository.existsById(3L)).thenReturn(true);
        when(destinoRepository.getReferenceById(3L)).thenReturn(destino);
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(usuario));
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocacion -> {
            Resena resena = invocacion.getArgument(0);
            resena.setId(11L);
            resena.setCreadaEn(LocalDateTime.now());
            return resena;
        });

        ResenaDto resultado = resenaService.crear(3L, crearDto("Un buen viaje", 4), null, CLIENTE);

        assertNull(resultado.getImagenUrl());
        verify(resenaRepository).save(any(Resena.class));
    }

    @Test
    void rechazaArchivoQueNoEsUnaImagenDeclarada() {
        when(destinoRepository.existsById(3L)).thenReturn(true);
        when(usuarioRepository.findById(8L)).thenReturn(
                Optional.of(Usuario.builder().id(8L).nombre("Ana").apellidos("Pérez").build()));
        MockMultipartFile archivo = new MockMultipartFile(
                "imagen", "texto.png", "image/png", "no es una imagen".getBytes());

        assertThrows(ArchivoImagenInvalidoException.class,
                () -> resenaService.crear(3L, crearDto("Comentario", 3), archivo, CLIENTE));
        verify(resenaRepository, never()).save(any(Resena.class));
    }

    private CrearResenaDto crearDto(String comentario, int calificacion) {
        CrearResenaDto dto = new CrearResenaDto();
        dto.setComentario(comentario);
        dto.setCalificacion(calificacion);
        return dto;
    }
}
