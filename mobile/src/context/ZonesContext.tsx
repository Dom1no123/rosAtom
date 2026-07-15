import { useRadiation } from "@/context/RadiationContext";
import { Zone } from "@/types";

interface ZonesContextValue {
  zones: Zone[];
  tick: number;
  triggerTestAlert: () => Promise<void>;
}

export function useZonesContext(): ZonesContextValue {
  const { snapshot, triggerAlert } = useRadiation();
  return {
    zones: snapshot.zones,
    tick: snapshot.tick,
    triggerTestAlert: async () => {
      await triggerAlert();
    },
  };
}
