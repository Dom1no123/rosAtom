import AsyncStorage from "@react-native-async-storage/async-storage";

import { BASE_STATIONS } from "@/data/stations";
import { BASE_ZONES } from "@/data/zones";
import {
  Alert,
  DailyStat,
  HistoryPoint,
  RadiationStatus,
  SimulationMode,
  Station,
  StationDetail,
  StatisticsResponse,
  Zone,
} from "@/types";
import { clampSafeLevel, isFutureIso, statusForLevel } from "@/utils/simulation";

const STORAGE_KEY = "@radiation_monitor_simulation_v3";
const VERSION = 3;
const UNIT = "мкЗв/ч";
const ALERT_DURATION_MS = 60_000;
const AUTO_MIN_MS = 2 * 60_000;
const AUTO_MAX_MS = 5 * 60_000;

export interface RadiationSnapshot {
  version: number;
  stations: Station[];
  zones: Zone[];
  histories: Record<string, HistoryPoint[]>;
  weekly: DailyStat[];
  alerts: Alert[];
  updatedAt: string;
  lastTickAt: number;
  lastStationUpdateAt: number;
  tick: number;
  simulationMode: SimulationMode;
  nextAutoAlertAt: number | null;
  nextAutoZoneId: string | null;
}

type Listener = (snapshot: RadiationSnapshot, newAlert?: Alert) => void;

function round(value: number): number {
  return Math.round(value * 1000) / 1000;
}

function safeLevel(current = 0.18): number {
  return clampSafeLevel(current, (Math.random() - 0.5) * 0.06);
}

function buildHistory(base: number, now: number): HistoryPoint[] {
  let level = base;
  return Array.from({ length: 24 }, (_, index) => {
    level = safeLevel(level);
    return {
      timestamp: new Date(now - (23 - index) * 60 * 60 * 1000).toISOString(),
      level,
      status: statusForLevel(level),
    };
  });
}

function buildWeekly(): DailyStat[] {
  return ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"].map((day) => {
    const average = round(0.13 + Math.random() * 0.14);
    return {
      day,
      average,
      min: round(Math.max(0.05, average - Math.random() * 0.05)),
      max: round(average + Math.random() * 0.12),
    };
  });
}

function scheduleNext(now: number): number {
  return now + AUTO_MIN_MS + Math.random() * (AUTO_MAX_MS - AUTO_MIN_MS);
}

function initialSnapshot(now = Date.now()): RadiationSnapshot {
  const stations = BASE_STATIONS.map((station, index) => {
    const level = safeLevel(0.11 + (index % 6) * 0.025);
    return { ...station, level, unit: UNIT, status: statusForLevel(level), lastUpdated: new Date(now).toISOString() };
  });
  const histories = Object.fromEntries(stations.map((station) => [station.id, buildHistory(station.level, now)]));
  return {
    version: VERSION,
    stations,
    zones: BASE_ZONES.map((zone) => ({
      id: zone.id,
      name: zone.name,
      centerLat: zone.centerLat,
      centerLon: zone.centerLon,
      level: zone.baseline,
      status: statusForLevel(zone.baseline),
      updatedAt: new Date(now).toISOString(),
    })),
    histories,
    weekly: buildWeekly(),
    alerts: [],
    updatedAt: new Date(now).toISOString(),
    lastTickAt: now,
    lastStationUpdateAt: now,
    tick: 0,
    simulationMode: "manual",
    nextAutoAlertAt: null,
    nextAutoZoneId: null,
  };
}

class LocalRadiationEngine {
  private state = initialSnapshot();
  private listeners = new Set<Listener>();
  private ready = false;

  async initialize(): Promise<RadiationSnapshot> {
    if (this.ready) return this.state;
    try {
      const raw = await AsyncStorage.getItem(STORAGE_KEY);
      const parsed = raw ? (JSON.parse(raw) as RadiationSnapshot) : null;
      if (parsed?.version === VERSION) this.state = parsed;
    } catch {
      this.state = initialSnapshot();
    }
    this.ready = true;
    await this.advance(Date.now());
    return this.state;
  }

  subscribe(listener: Listener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  getSnapshot(): RadiationSnapshot {
    return this.state;
  }

  async refresh(): Promise<void> {
    await this.advance(Date.now(), true);
  }

  async advance(now = Date.now(), force = false, stationIntervalMs = 60_000): Promise<void> {
    const elapsed = now - this.state.lastTickAt;
    if (!force && elapsed < 4_000) return;

    let newAlert: Alert | undefined;
    const active = this.state.alerts.find((alert) => alert.status === "critical" && isFutureIso(alert.expiresAt, now));
    let zones = this.state.zones.map((zone) => {
      const zoneAlert = this.state.alerts.find((alert) => alert.targetId === zone.id && isFutureIso(alert.expiresAt, now));
      if (zoneAlert) return zone;
      const level = safeLevel(Math.min(zone.level, 0.28));
      return { ...zone, level, status: statusForLevel(level), updatedAt: new Date(now).toISOString() };
    });

    let nextAutoAlertAt = this.state.nextAutoAlertAt;
    let nextAutoZoneId = this.state.nextAutoZoneId;
    if (this.state.simulationMode === "automatic" && !active && (!nextAutoAlertAt || now >= nextAutoAlertAt)) {
      const result = this.createAlert(zones, now, nextAutoZoneId ?? undefined);
      zones = result.zones;
      newAlert = result.alert;
      nextAutoAlertAt = scheduleNext(now + ALERT_DURATION_MS);
      nextAutoZoneId = zones[Math.floor(Math.random() * zones.length)].id;
    }

    const updateStations = force || now - this.state.lastStationUpdateAt >= stationIntervalMs;
    const stations = updateStations
      ? this.state.stations.map((station) => {
          const level = safeLevel(station.level);
          return { ...station, level, status: statusForLevel(level), lastUpdated: new Date(now).toISOString() };
        })
      : this.state.stations;
    const histories = { ...this.state.histories };
    if (updateStations) {
      stations.forEach((station) => {
        const previous = histories[station.id] ?? [];
        const latest = previous[previous.length - 1];
        const point = { timestamp: new Date(now).toISOString(), level: station.level, status: station.status };
        histories[station.id] = latest && now - new Date(latest.timestamp).getTime() < 60 * 60_000
          ? [...previous.slice(0, -1), point]
          : [...previous.slice(-23), point];
      });
    }

    this.state = {
      ...this.state,
      stations,
      zones,
      histories,
      alerts: newAlert ? [newAlert, ...this.state.alerts].slice(0, 50) : this.state.alerts,
      updatedAt: new Date(now).toISOString(),
      lastTickAt: now,
      lastStationUpdateAt: updateStations ? now : this.state.lastStationUpdateAt,
      tick: this.state.tick + 1,
      nextAutoAlertAt,
      nextAutoZoneId,
    };
    await this.publish(newAlert);
  }

  async triggerAlert(zoneId?: string): Promise<Alert> {
    const now = Date.now();
    const active = this.state.alerts.find((alert) => isFutureIso(alert.expiresAt, now));
    if (active) return active;
    const result = this.createAlert(this.state.zones, now, zoneId);
    this.state = {
      ...this.state,
      zones: result.zones,
      alerts: [result.alert, ...this.state.alerts].slice(0, 50),
      updatedAt: new Date(now).toISOString(),
      tick: this.state.tick + 1,
    };
    await this.publish(result.alert);
    return result.alert;
  }

  async acknowledgeAlert(alertId: string): Promise<void> {
    this.state = { ...this.state, alerts: this.state.alerts.map((alert) => alert.id === alertId ? { ...alert, acknowledged: true } : alert) };
    await this.publish();
  }

  async setSimulationMode(mode: SimulationMode): Promise<void> {
    this.state = {
      ...this.state,
      simulationMode: mode,
      nextAutoAlertAt: mode === "automatic" ? scheduleNext(Date.now()) : null,
      nextAutoZoneId: mode === "automatic"
        ? this.state.zones[Math.floor(Math.random() * this.state.zones.length)].id
        : null,
    };
    await this.publish();
  }

  getStationDetail(id: string): StationDetail | null {
    const station = this.state.stations.find((item) => item.id === id);
    if (!station) return null;
    const safe = station.status === "normal" || station.status === "elevated";
    const recommendations: Record<RadiationStatus, string> = {
      normal: "Радиационный фон в норме. Особые меры предосторожности не требуются.",
      elevated: "Уровень немного повышен. Следите за обновлениями и сократите длительное пребывание на улице.",
      dangerous: "Опасный уровень радиации. Ограничьте пребывание вне помещений.",
      critical: "Критический уровень радиации. Следуйте инструкциям экстренных служб.",
    };
    return { ...station, history: this.state.histories[id] ?? [], safe, recommendation: recommendations[station.status] };
  }

  getStatistics(): StatisticsResponse {
    const levels = this.state.stations.map((station) => station.level);
    return {
      average: round(levels.reduce((sum, value) => sum + value, 0) / levels.length),
      min: Math.min(...levels),
      max: Math.max(...levels),
      unit: UNIT,
      weekly: this.state.weekly,
      updatedAt: this.state.updatedAt,
    };
  }

  private createAlert(zones: Zone[], now: number, zoneId?: string): { zones: Zone[]; alert: Alert } {
    const target = zones.find((zone) => zone.id === zoneId) ?? zones[Math.floor(Math.random() * zones.length)];
    const expiresAt = new Date(now + ALERT_DURATION_MS).toISOString();
    const alert: Alert = {
      id: `zone-alert-${target.id}-${now}`,
      targetType: "zone",
      targetId: target.id,
      targetName: target.name,
      level: 1.5,
      status: "critical",
      message: `Критический уровень радиации в зоне «${target.name}». Следуйте инструкции по безопасности.`,
      createdAt: new Date(now).toISOString(),
      startedAt: new Date(now).toISOString(),
      expiresAt,
      acknowledged: false,
    };
    return {
      alert,
      zones: zones.map((zone) => zone.id === target.id ? { ...zone, level: alert.level, status: "critical", updatedAt: alert.startedAt } : zone),
    };
  }

  private async publish(newAlert?: Alert): Promise<void> {
    try {
      await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(this.state));
    } catch {
      // The in-memory simulator remains usable even if device storage is full.
    }
    this.listeners.forEach((listener) => listener(this.state, newAlert));
  }
}

export const localRadiationEngine = new LocalRadiationEngine();
