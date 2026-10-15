import React from 'react';
import { FiltroDestinoParams } from '../types/destino';

interface Props {
  filtros: FiltroDestinoParams;
  onFilterChange: (nuevosFiltros: FiltroDestinoParams) => void;
}

export const FiltrosDestino: React.FC<Props> = ({ filtros, onFilterChange }) => {
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;

    if (type === 'checkbox') {
      const checked = (e.target as HTMLInputElement).checked;
      onFilterChange({ ...filtros, [name]: checked });
    } else {
      onFilterChange({ ...filtros, [name]: value });
    }
  };

  return (
    <div className="filtros-bar">
      {/* Búsqueda por palabra clave */}
      <input
        type="text"
        name="q"
        placeholder="Buscar destino o ciudad..."
        value={filtros.q || ''}
        onChange={handleChange}
      />

      {/* Filtro Dificultad */}
      <select
        name="dificultad"
        value={filtros.dificultad || ''}
        onChange={handleChange}
      >
        <option value="">Todas las dificultades</option>
        <option value="BAJA">Baja</option>
        <option value="MODERADA">Moderada</option>
        <option value="ALTA">Alta</option>
      </select>

      {/* Filtro Accesibilidad */}
      <label>
        <input
          type="checkbox"
          name="accesibilidad"
          checked={!!filtros.accesibilidad}
          onChange={handleChange}
        />
        Solo accesibilidad adaptada
      </label>
    </div>
  );
};