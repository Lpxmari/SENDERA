/** Íconos decorativos de servicios y datos del alojamiento (el texto que los acompaña es el que se lee). */
const IconoServicio = ({ id }: { id: string }) => {
    const props = {
        viewBox: '0 0 24 24',
        width: 18,
        height: 18,
        fill: 'none',
        stroke: 'currentColor',
        strokeWidth: 1.8,
        strokeLinecap: 'round' as const,
        strokeLinejoin: 'round' as const,
        'aria-hidden': true,
    };
    switch (id) {
        case 'piscina':
            return (
                <svg {...props}>
                    <path d="M3 17c2 0 2-1.5 4.5-1.5S10 17 12 17s2.5-1.5 4.5-1.5S19 17 21 17" />
                    <path d="M3 21c2 0 2-1.5 4.5-1.5S10 21 12 21s2.5-1.5 4.5-1.5S19 21 21 21" />
                    <path d="M8 13V6a2 2 0 0 1 4 0M16 13V6a2 2 0 0 0-4 0M8 9h8" />
                </svg>
            );
        case 'desayuno':
            return (
                <svg {...props}>
                    <path d="M4 9h13v5a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5V9Z" />
                    <path d="M17 10h1.5a2.5 2.5 0 0 1 0 5H17" />
                    <path d="M8 3v3M12 3v3" />
                </svg>
            );
        case 'wifi':
            return (
                <svg {...props}>
                    <path d="M2 9a15 15 0 0 1 20 0M5 12.5a10 10 0 0 1 14 0M8.5 16a5 5 0 0 1 7 0" />
                    <circle cx="12" cy="19" r="1" />
                </svg>
            );
        case 'ubicacion':
            return (
                <svg {...props}>
                    <path d="M12 21s7-6.2 7-11.5A7 7 0 0 0 5 9.5C5 14.8 12 21 12 21Z" />
                    <circle cx="12" cy="9.5" r="2.5" />
                </svg>
            );
        case 'accesibilidad':
            return (
                <svg {...props}>
                    <circle cx="12" cy="4" r="2" />
                    <path d="M12 6v6m0 0l-3 6m3-6l3 6M6 10h12" />
                </svg>
            );
        case 'dificultad':
            return (
                <svg {...props}>
                    <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" />
                    <path d="M4 22v-7" />
                </svg>
            );
        case 'precio':
            return (
                <svg {...props}>
                    <path d="M3 12V4h8l10 10-8 8L3 12Z" />
                    <circle cx="7.5" cy="8.5" r="1" />
                </svg>
            );
        case 'estacionamiento':
            return (
                <svg {...props}>
                    <rect x="4" y="4" width="16" height="16" rx="3" />
                    <path d="M10 17V8h3a2.5 2.5 0 0 1 0 5h-3" />
                </svg>
            );
        default:
            return (
                <svg {...props}>
                    <circle cx="12" cy="12" r="9" />
                    <path d="M9.5 16.5V8h3.2a2.6 2.6 0 0 1 0 5.2H9.5" />
                </svg>
            );
    }
};

export default IconoServicio;
