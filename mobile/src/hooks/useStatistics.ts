import { useRadiation } from "@/context/RadiationContext";
import { localRadiationEngine } from "@/services/localRadiationEngine";

export function useStatistics() {
  const { snapshot, loading, refresh } = useRadiation();
  void snapshot;
  return { statistics: localRadiationEngine.getStatistics(), loading, refresh };
}
