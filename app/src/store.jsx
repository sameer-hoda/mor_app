import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { get, post } from './api.js';

const Ctx = createContext(null);
export const useStore = () => useContext(Ctx);

export function StoreProvider({ children }) {
  const [profile, setProfile] = useState(null);
  const [home, setHome] = useState(null);
  const [snack, setSnack] = useState(null);

  const refreshHome = useCallback(async () => {
    try { setHome(await get('/home')); } catch { /* backend down */ }
  }, []);

  const refreshProfile = useCallback(async () => {
    try { setProfile(await get('/profile')); } catch { /* backend down */ }
  }, []);

  useEffect(() => { refreshProfile(); refreshHome(); }, [refreshProfile, refreshHome]);

  const showSnack = useCallback((msg, kind = 'ok') => {
    setSnack({ msg, kind });
    setTimeout(() => setSnack(null), 2600);
  }, []);

  const saveProfile = useCallback(async (patch) => {
    const p = await post('/profile', patch);
    setProfile(p);
    return p;
  }, []);

  const value = { profile, home, refreshHome, refreshProfile, saveProfile, snack, showSnack, setProfile };
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
