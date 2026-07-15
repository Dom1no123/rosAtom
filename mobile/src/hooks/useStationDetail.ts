import { useRadiation } from "@/context/RadiationContext";

export function useStationDetail(id: string) {
  const { snapshot, loading, refresh, getStationDetail } = useRadiation();
  void snapshot;
  return { station: getStationDetail(id), loading, refresh };
}
