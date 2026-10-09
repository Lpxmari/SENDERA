import { createContext, ReactNode, useCallback, useContext, useMemo, useState } from 'react';
import { AuthResponse, LoginRequest, RegistroRequest, Usuario } from '../types/auth';
import {
    borrarSesion,
    guardarSesion,
    iniciarSesion,
    leerSesion,
    registrarUsuario,
} from '../services/authService';

interface AuthContextValue {
    usuario: Usuario | null;
    autenticado: boolean;
    login: (datos: LoginRequest) => Promise<void>;
    registro: (datos: RegistroRequest) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [usuario, setUsuario] = useState<Usuario | null>(() => leerSesion()?.usuario ?? null);

    const abrirSesion = (sesion: AuthResponse) => {
        guardarSesion(sesion);
        setUsuario(sesion.usuario);
    };

    const login = useCallback(async (datos: LoginRequest) => {
        abrirSesion(await iniciarSesion(datos));
    }, []);

    const registro = useCallback(async (datos: RegistroRequest) => {
        abrirSesion(await registrarUsuario(datos));
    }, []);

    const logout = useCallback(() => {
        borrarSesion();
        setUsuario(null);
    }, []);

    const value = useMemo(
        () => ({ usuario, autenticado: usuario !== null, login, registro, logout }),
        [usuario, login, registro, logout],
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextValue => {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider');
    return ctx;
};
