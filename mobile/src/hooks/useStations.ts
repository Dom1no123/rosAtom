import { useCallback, useState } from "react";
import { useRadiation } from "@/context/RadiationContext";

export function useStations(refreshIntervalSec = 60) {
  void refreshIntervalSec;
  const { snapshot, loading, refresh: refreshEngine } = useRadiation();
  const [refreshing, setRefreshing] = useState(false);

  const refresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await refreshEngine();
    } finally {
      setRefreshing(false);
    }
  }, [refreshEngine]);

  return {
    stations: snapshot.stations,
    updatedAt: snapshot.updatedAt,
    loading,
    refreshing,
    refresh,
  };
}
