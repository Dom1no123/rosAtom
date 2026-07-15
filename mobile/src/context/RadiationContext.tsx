import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { AppState } from "react-native";

import { EmergencyModal } from "@/components/EmergencyModal";
import {
  localRadiationEngine,
  RadiationSnapshot,
} from "@/services/localRadiationEngine";
import { Alert, SimulationMode, StationDetail } from "@/types";
import { useAppTheme } from "@/theme/ThemeContext";
import {
  addNotificationResponseListener,
  cancelAutomaticNotifications,
  scheduleAutomaticNotification,
  showAlertNotification,
} from "@/services/localNotifications";
import { openZoneFromNotification } from "@/navigation/navigationRef";

interface RadiationContextValue {
  snapshot: RadiationSnapshot;
  loading: boolean;
  refresh: () => Promise<void>;
  triggerAlert: (zoneId?: string) => Promise<Alert>;
  setSimulationMode: (mode: SimulationMode) => Promise<void>;
  getStationDetail: (id: string) => StationDetail | null;
}

const RadiationContext = createContext<RadiationContextValue | undefined>(undefined);

export function RadiationProvider({ children }: { children: React.ReactNode }) {
  const { notificationsEnabled, refreshIntervalSec } = useAppTheme();
  const [snapshot, setSnapshot] = useState(localRadiationEngine.getSnapshot());
  const [loading, setLoading] = useState(true);
  const [visibleAlert, setVisibleAlert] = useState<Alert | null>(null);

  useEffect(() => {
    const unsubscribe = localRadiationEngine.subscribe((next, newAlert) => {
      setSnapshot(next);
      if (newAlert && !newAlert.acknowledged) {
        setVisibleAlert(newAlert);
        if (notificationsEnabled) showAlertNotification(newAlert).catch(() => {});
      }
    });
    localRadiationEngine.initialize().then((next) => {
      setSnapshot(next);
      const active = next.alerts.find(
        (alert) =>
          !alert.acknowledged &&
          alert.expiresAt &&
          new Date(alert.expiresAt).getTime() > Date.now()
      );
      if (active) setVisibleAlert(active);
      setLoading(false);
    });
    const timer = setInterval(
      () => localRadiationEngine.advance(Date.now(), false, refreshIntervalSec * 1000),
      5_000
    );
    const appState = AppState.addEventListener("change", (state) => {
      if (state === "active") localRadiationEngine.advance(Date.now(), true);
    });
    const notificationResponse = addNotificationResponseListener((response) => {
      const zoneId = response.notification.request.content.data?.zoneId;
      openZoneFromNotification(typeof zoneId === "string" ? zoneId : undefined);
    });
    return () => {
      unsubscribe();
      clearInterval(timer);
      appState.remove();
      notificationResponse.remove();
    };
  }, [notificationsEnabled, refreshIntervalSec]);

  useEffect(() => {
    if (notificationsEnabled && snapshot.simulationMode === "automatic" && snapshot.nextAutoAlertAt) {
      scheduleAutomaticNotification(snapshot.nextAutoAlertAt, snapshot.nextAutoZoneId ?? undefined).catch(() => {});
    } else {
      cancelAutomaticNotifications().catch(() => {});
    }
  }, [notificationsEnabled, snapshot.simulationMode, snapshot.nextAutoAlertAt, snapshot.nextAutoZoneId]);

  const dismissAlert = useCallback(async () => {
    if (visibleAlert) await localRadiationEngine.acknowledgeAlert(visibleAlert.id);
    setVisibleAlert(null);
  }, [visibleAlert]);

  const value = useMemo<RadiationContextValue>(
    () => ({
      snapshot,
      loading,
      refresh: () => localRadiationEngine.refresh(),
      triggerAlert: (zoneId) => localRadiationEngine.triggerAlert(zoneId),
      setSimulationMode: (mode) => localRadiationEngine.setSimulationMode(mode),
      getStationDetail: (id) => localRadiationEngine.getStationDetail(id),
    }),
    [snapshot, loading]
  );

  const alertZone = visibleAlert
    ? snapshot.zones.find((zone) => zone.id === visibleAlert.targetId) ?? null
    : null;

  return (
    <RadiationContext.Provider value={value}>
      {children}
      <EmergencyModal zone={alertZone} onDismiss={dismissAlert} />
    </RadiationContext.Provider>
  );
}

export function useRadiation(): RadiationContextValue {
  const context = useContext(RadiationContext);
  if (!context) throw new Error("useRadiation must be used within RadiationProvider");
  return context;
}
