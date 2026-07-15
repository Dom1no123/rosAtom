export type RadiationStatus = "normal" | "elevated" | "dangerous" | "critical";

export interface Station {
  id: string;
  name: string;
  region: string;
  latitude: number;
  longitude: number;
  level: number;
  unit: string;
  status: RadiationStatus;
  lastUpdated: string;
}

export interface HistoryPoint {
  timestamp: string;
  level: number;
  status: RadiationStatus;
}

export interface StationDetail extends Station {
  history: HistoryPoint[];
  recommendation: string;
  safe: boolean;
}

export interface StationsResponse {
  count: number;
  updatedAt: string;
  stations: Station[];
}

export interface DailyStat {
  day: string;
  average: number;
  max: number;
  min: number;
}

export interface StatisticsResponse {
  average: number;
  max: number;
  min: number;
  unit: string;
  weekly: DailyStat[];
  updatedAt: string;
}

export interface Alert {
  id: string;
  targetType: "station" | "zone";
  targetId: string;
  targetName: string;
  level: number;
  status: RadiationStatus;
  message: string;
  createdAt: string;
  startedAt: string;
  expiresAt?: string;
  acknowledged: boolean;
}

export interface AlertsResponse {
  count: number;
  alerts: Alert[];
}

export type ThemeMode = "light" | "dark";
export type Language = "ru" | "kk" | "en";
export type SimulationMode = "manual" | "automatic";

export interface Zone {
  id: string;
  name: string;
  centerLat: number;
  centerLon: number;
  level: number;
  status: RadiationStatus;
  updatedAt: string;
}

export interface ZonesResponse {
  zones: Zone[];
  updatedAt: string;
}

export interface AppSettings {
  themeMode: ThemeMode;
  language: Language;
  notificationsEnabled: boolean;
  refreshIntervalSec: number;
}
