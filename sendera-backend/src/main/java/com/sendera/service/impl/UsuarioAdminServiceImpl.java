package com.sendera.service.impl;

import com.sendera.config.SecurityConfig;
import com.sendera.dto.CambiarActivoDto;
import com.sendera.dto.CambiarRolDto;
import com.sendera.dto.PaginaDto;
import com.sendera.dto.UsuarioAdminDto;
import com.sendera.exception.FiltroInvalidoException;
import com.sendera.exception.OperacionAdminNoPermitidaException;
import com.sendera.exception.RecursoNoEncontradoException;
import com.sendera.model.Usuario;
import com.sendera.repository.UsuarioRepository;
import com.sendera.security.UsuarioActual;
import com.sendera.service.AuditoriaService;
import com.sendera.service.UsuarioAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioAdminServiceImpl implements UsuarioAdminService {

    private static final Set<String> ROLES = Set.of(SecurityConfig.ROL_ADMIN, "ROLE_CLIENTE");
    private static final int TAMANO_MAXIMO = 100;

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public PaginaDto<UsuarioAdminDto> listar(String q, String rol, Boolean activo, int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new FiltroInvalidoException("La página debe ser 0 o mayor y el tamaño estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
        String rolFiltro = rol == null || rol.isBlank() ? null : rol.trim().toUpperCase(Locale.ROOT);
        if (rolFiltro != null && !ROLES.contains(rolFiltro)) {
            throw new FiltroInvalidoException("El rol debe ser ROLE_ADMIN o ROLE_CLIENTE.");
        }
        Page<Usuario> resultado = usuarioRepository.buscar(patron(q), rolFiltro, activo,
                PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaRegistro").and(Sort.by("id"))));
        return PaginaDto.<UsuarioAdminDto>builder()
                .contenido(resultado.getContent().stream().map(this::convertir).toList())
                .pagina(pagina)
                .tamano(tamano)
                .total(resultado.getTotalElements())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioAdminDto obtener(Long id) {
        return convertir(buscar(id));
    }

    @Override
    @Transactional
    public UsuarioAdminDto cambiarRol(Long id, CambiarRolDto dto, UsuarioActual actor) {
        Usuario usuario = buscar(id);
        if (actor.id().equals(id)) {
            throw new OperacionAdminNoPermitidaException("No puedes cambiar tu propio rol.");
        }
        String anterior = usuario.getRol();
        if (anterior.equals(dto.getRol())) {
            return convertir(usuario);
        }
        if (SecurityConfig.ROL_ADMIN.equals(anterior) && usuario.isActivo() && ultimoAdministrador()) {
            throw new OperacionAdminNoPermitidaException("Debe quedar al menos un administrador activo.");
        }
        usuario.setRol(dto.getRol());
        usuarioRepository.save(usuario);
        auditoriaService.registrar(actor, "USUARIO_CAMBIAR_ROL", "usuario", id, anterior + " -> " + dto.getRol());
        return convertir(usuario);
    }

    @Override
    @Transactional
    public UsuarioAdminDto cambiarActivo(Long id, CambiarActivoDto dto, UsuarioActual actor) {
        Usuario usuario = buscar(id);
        if (actor.id().equals(id)) {
            throw new OperacionAdminNoPermitidaException("No puedes desactivar tu propia cuenta.");
        }
        boolean nuevo = Boolean.TRUE.equals(dto.getActivo());
        if (usuario.isActivo() == nuevo) {
            return convertir(usuario);
        }
        if (!nuevo && SecurityConfig.ROL_ADMIN.equals(usuario.getRol()) && ultimoAdministrador()) {
            throw new OperacionAdminNoPermitidaException("Debe quedar al menos un administrador activo.");
        }
        usuario.setActivo(nuevo);
        usuarioRepository.save(usuario);
        auditoriaService.registrar(actor, nuevo ? "USUARIO_ACTIVAR" : "USUARIO_DESACTIVAR", "usuario", id, null);
        return convertir(usuario);
    }

    private boolean ultimoAdministrador() {
        return usuarioRepository.countByRolAndActivoTrue(SecurityConfig.ROL_ADMIN) <= 1;
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));
    }

    /** Minúsculas y comodines escapados (con '!') para que el texto del administrador no actúe como patrón LIKE. */
    private String patron(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String escapado = q.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escapado + "%";
    }

    private UsuarioAdminDto convertir(Usuario u) {
        return UsuarioAdminDto.builder()
                .id(u.getId())
                .nombre(u.getNombre())
                .apellidos(u.getApellidos())
                .correo(u.getCorreo())
                .rol(u.getRol())
                .activo(u.isActivo())
                .fechaRegistro(u.getFechaRegistro())
                .build();
    }
}
