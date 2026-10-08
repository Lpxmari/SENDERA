import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Encabezado from './components/Encabezado';
import Home from './pages/Home';
import DetalleDestino from './pages/DetalleDestino';
import FormularioReserva from './pages/FormularioReserva';
import ConfirmacionReserva from './pages/ConfirmacionReserva';
import './App.css';

function App() {
    return (
        <Router>
            <div className="app-container">
                <Encabezado />
                <main>
                    <Routes>
                        <Route path="/" element={<Home />} />
                        <Route path="/destinos/:id" element={<DetalleDestino />} />
                        <Route path="/reservar/:destinoId" element={<FormularioReserva />} />
                        <Route path="/confirmacion" element={<ConfirmacionReserva />} />
                    </Routes>
                </main>
            </div>
        </Router>
    );
}

export default App;