import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import DetalleDestino from './pages/DetalleDestino';
import FormularioReserva from './pages/FormularioReserva';
import ConfirmacionReserva from './pages/ConfirmacionReserva';

function App() {
  return (
      <Router>
        <div className="app-container">
          <header>
            <h1>Sendera</h1>
          </header>
          <main>
            <Routes>
              <Route path="/" element={<Home />} />
              { <Route path="/destinos/:id" element={<DetalleDestino />} /> }
              { <Route path="/reservar/:destinoId" element={<FormularioReserva />} /> }
              { <Route path="/confirmacion" element={<ConfirmacionReserva />} /> }
            </Routes>
          </main>
        </div>
      </Router>
  );
}

export default App;