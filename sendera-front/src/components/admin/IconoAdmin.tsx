const TRAZOS = {
    usuarios:
        'M16 20v-1a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v1M9.5 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zM21 20v-1a4 4 0 0 0-3-3.9M16 4.1a3.5 3.5 0 0 1 0 6.8',
    destinos: 'M12 21s7-6.2 7-11.5A7 7 0 0 0 5 9.5C5 14.8 12 21 12 21zM12 12a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z',
    reservas: 'M4 6h16v14H4zM4 10h16M8 3v4M16 3v4M8 14h3',
    resenas: 'M21 12a8 8 0 0 1-11.6 7.1L4 20l1.1-4.6A8 8 0 1 1 21 12zM8.5 11.5h7M8.5 14.5h4',
    salir: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
    mas: 'M12 5v14M5 12h14',
    lapiz: 'M4 20h4L19 9a2.1 2.1 0 0 0-3-3L5 17v3zM14 7l3 3',
    papelera: 'M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4h6v3',
    escudo: 'M12 3l8 3v6c0 4.5-3.2 8-8 9-4.8-1-8-4.5-8-9V6l8-3zM9 12l2 2 4-4',
    usuario: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21a8 8 0 0 1 16 0',
    ban: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM5.6 5.6l12.8 12.8',
    check: 'M5 12.5l4.5 4.5L19 7.5',
    equis: 'M6 6l12 12M18 6L6 18',
    izquierda: 'M15 5l-7 7 7 7',
    derecha: 'M9 5l7 7-7 7',
    imagen: 'M4 5h16v14H4zM8.5 10a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zM4 17l5-5 4 4 3-3 4 4',
    lupa: 'M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM20 20l-4-4',
    limpiar: 'M4 12a8 8 0 1 1 2.5 5.8M4 19v-5h5',
    compartir:
        'M18 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM6 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM18 22a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM8.6 13.5l6.8 4M15.4 6.5l-6.8 4',
    guardar: 'M6 4h12v17l-6-4-6 4V4z',
    campana: 'M6 9a6 6 0 1 1 12 0c0 6 2 7 2 7H4s2-1 2-7zM10 20a2 2 0 0 0 4 0',
    sitio: 'M3 12l9-8 9 8M5 10v10h5v-6h4v6h5V10',
    ordenar: 'M8 9l4-4 4 4M8 15l4 4 4-4',
    reloj: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2',
    alerta: 'M12 3l10 18H2L12 3zM12 10v4M12 17.5v.5',
    ruta: 'M6 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4zM18 8a2 2 0 1 0 0-4 2 2 0 0 0 0 4zM8 18h7a3 3 0 0 0 0-6H9a3 3 0 0 1 0-6h7',
} as const;

export type NombreIconoAdmin = keyof typeof TRAZOS;

/** Ícono decorativo (el texto que lo acompaña es el que se lee). */
const IconoAdmin = ({ nombre, relleno = false }: { nombre: NombreIconoAdmin; relleno?: boolean }) => (
    <svg
        viewBox="0 0 24 24"
        width="20"
        height="20"
        fill={relleno ? 'currentColor' : 'none'}
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
        focusable="false"
    >
        <path d={TRAZOS[nombre]} />
    </svg>
);

export default IconoAdmin;
