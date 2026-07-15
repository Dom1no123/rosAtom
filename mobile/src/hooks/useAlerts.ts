import { useRadiation } from "@/context/RadiationContext";

export function useAlerts() {
  const { snapshot, loading, refresh } = useRadiation();
  return { alerts: snapshot.alerts, loading, refresh };
}
