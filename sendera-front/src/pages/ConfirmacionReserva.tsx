import { Link } from 'react-router-dom';

const ConfirmacionReserva = () => {
    return (
        <div style={{ textAlign: 'center', marginTop: '50px', padding: '20px' }}>
            <h2 style={{ color: '#4CAF50' }}>¡Reserva Creada Exitosamente!</h2>
            <p>Tu reserva ha sido registrada en el sistema con estado <strong>PENDIENTE</strong> de pago.</p>
            <Link to="/">
                <button style={{ marginTop: '20px', padding: '10px 20px', cursor: 'pointer', backgroundColor: '#eee', border: '1px solid #ccc', borderRadius: '4px' }}>
                    Volver al Catálogo
                </button>
            </Link>
        </div>
    );
};

export default ConfirmacionReserva;