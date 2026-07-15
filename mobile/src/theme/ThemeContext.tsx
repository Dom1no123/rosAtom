import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { Appearance } from "react-native";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { AppColors, darkColors, lightColors } from "./colors";
import { AppSettings, Language, ThemeMode } from "@/types";

const STORAGE_KEY = "@radiation_monitor_settings";

interface ThemeContextValue {
  colors: AppColors;
  themeMode: ThemeMode;
  language: Language;
  notificationsEnabled: boolean;
  refreshIntervalSec: number;
  isLoaded: boolean;
  setThemeMode: (mode: ThemeMode) => void;
  setLanguage: (lang: Language) => void;
  setNotificationsEnabled: (v: boolean) => void;
  setRefreshIntervalSec: (v: number) => void;
}

const defaultSettings: AppSettings = {
  themeMode: Appearance.getColorScheme() === "dark" ? "dark" : "light",
  language: "ru",
  notificationsEnabled: false,
  refreshIntervalSec: 60,
};

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [settings, setSettings] = useState<AppSettings>(defaultSettings);
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const raw = await AsyncStorage.getItem(STORAGE_KEY);
        if (raw) {
          setSettings({ ...defaultSettings, ...JSON.parse(raw) });
        }
      } catch (e) {
        // ignore, fall back to defaults
      } finally {
        setIsLoaded(true);
      }
    })();
  }, []);

  const persist = async (next: AppSettings) => {
    setSettings(next);
    try {
      await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    } catch (e) {
      // ignore persistence errors
    }
  };

  const value = useMemo<ThemeContextValue>(
    () => ({
      colors: settings.themeMode === "dark" ? darkColors : lightColors,
      themeMode: settings.themeMode,
      language: settings.language,
      notificationsEnabled: settings.notificationsEnabled,
      refreshIntervalSec: settings.refreshIntervalSec,
      isLoaded,
      setThemeMode: (mode) => persist({ ...settings, themeMode: mode }),
      setLanguage: (lang) => persist({ ...settings, language: lang }),
      setNotificationsEnabled: (v) => persist({ ...settings, notificationsEnabled: v }),
      setRefreshIntervalSec: (v) => persist({ ...settings, refreshIntervalSec: v }),
    }),
    [settings, isLoaded]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export function useAppTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useAppTheme must be used within ThemeProvider");
  return ctx;
}
