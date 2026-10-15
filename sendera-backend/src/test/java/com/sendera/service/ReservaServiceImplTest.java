package com.sendera.service;

import com.sendera.dto.CrearReservaDto;
import com.sendera.dto.ReservaDto;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.exception.ReservaNoPermitidaException;
import com.sendera.model.EstadoReserva;
import com.sendera.model.Itinerario;
import com.sendera.model.Reserva;
import com.sendera.model.Usuario;
import com.sendera.repository.ItinerarioRepository;
import com.sendera.repository.ReservaRepository;
import com.sendera.repository.UsuarioRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.impl.ReservaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    private static final UsuarioActual CLIENTE = new UsuarioActual(5L, false);
    private static final UsuarioActual ADMIN = new UsuarioActual(1L, true);

    @Mock
    ReservaRepository reservaRepository;
    @Mock
    ItinerarioRepository itinerarioRepository;
    @Mock
    UsuarioRepository usuarioRepository;
    @InjectMocks
    ReservaServiceImpl reservaService;

    private Itinerario itinerario;

    @BeforeEach
    void setUp() {
        itinerario = Itinerario.builder().id(7L).precioBase(new BigDecimal("435000.00")).cuposDisponibles(2).build();
    }

    private CrearReservaDto dto() {
        CrearReservaDto dto = new CrearReservaDto();
        dto.setItinerarioId(7L);
        return dto;
    }

    @Test
    void crearAsignaElPropietarioDelTokenCalculaMontoYDescuentaCupo() {
        Usuario propietario = Usuario.builder().id(5L).build();
        when(itinerarioRepository.buscarParaReservar(7L)).thenReturn(Optional.of(itinerario));
        when(usuarioRepository.getReferenceById(5L)).thenReturn(propietario);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        ReservaDto resultado = reservaService.crear(dto(), CLIENTE);

        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(guardada.capture());
        assertEquals(propietario, guardada.getValue().getUsuario());
        assertEquals(new BigDecimal("435000.00"), resultado.getMontoTotal());
        assertEquals("PENDIENTE", resultado.getEstadoReserva());
        assertNotNull(resultado.getFechaReserva());
        assertEquals(1, itinerario.getCuposDisponibles());
    }

    @Test
    void crearConItinerarioInexistenteLanzaNoEncontrado() {
        when(itinerarioRepository.buscarParaReservar(7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.crear(dto(), CLIENTE));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crearSinCuposLanzaConflicto() {
        itinerario.setCuposDisponibles(0);
        when(itinerarioRepository.buscarParaReservar(7L)).thenReturn(Optional.of(itinerario));

        assertThrows(ReservaNoPermitidaException.class, () -> reservaService.crear(dto(), CLIENTE));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cancelarDevuelveElCupo() {
        Reserva reserva = Reserva.builder().id(1L).itinerario(itinerario).estadoReserva(EstadoReserva.PENDIENTE).build();
        when(reservaRepository.findByIdAndUsuarioId(1L, 5L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        ReservaDto resultado = reservaService.cancelar(1L, CLIENTE);

        assertEquals("CANCELADA", resultado.getEstadoReserva());
        assertEquals(3, itinerario.getCuposDisponibles());
    }

    @Test
    void cancelarDosVecesLanzaConflicto() {
        Reserva reserva = Reserva.builder().id(1L).itinerario(itinerario).estadoReserva(EstadoReserva.CANCELADA).build();
        when(reservaRepository.findByIdAndUsuarioId(1L, 5L)).thenReturn(Optional.of(reserva));

        assertThrows(ReservaNoPermitidaException.class, () -> reservaService.cancelar(1L, CLIENTE));
        assertEquals(2, itinerario.getCuposDisponibles());
    }

    @Test
    void cancelarReservaAjenaSeTrataComoInexistente() {
        when(reservaRepository.findByIdAndUsuarioId(9L, 5L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.cancelar(9L, CLIENTE));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void clienteSoloListaSusReservasYAdminListaTodas() {
        when(reservaRepository.findByUsuarioIdOrderByIdAsc(5L)).thenReturn(List.of());
        when(reservaRepository.findAll()).thenReturn(List.of());

        reservaService.listar(CLIENTE);
        verify(reservaRepository, never()).findAll();

        reservaService.listar(ADMIN);
        verify(reservaRepository).findAll();
    }

    @Test
    void adminPuedeConsultarCualquierReserva() {
        Reserva reserva = Reserva.builder().id(3L).itinerario(itinerario).estadoReserva(EstadoReserva.PENDIENTE).build();
        when(reservaRepository.findById(3L)).thenReturn(Optional.of(reserva));

        assertEquals(3L, reservaService.obtenerPorId(3L, ADMIN).getId());
        verify(reservaRepository, never()).findByIdAndUsuarioId(any(), any());
    }
}
