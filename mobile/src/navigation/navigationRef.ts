import { createNavigationContainerRef, NavigatorScreenParams } from "@react-navigation/native";
import { RootStackParamList, TabParamList } from "./types";

export const navigationRef = createNavigationContainerRef<RootStackParamList>();
let pendingZoneId: string | undefined;

export function openZoneFromNotification(zoneId?: string) {
  if (!navigationRef.isReady()) {
    pendingZoneId = zoneId;
    return;
  }
  navigationRef.navigate("Tabs", {
    screen: "MapTab",
    params: zoneId ? { zoneId } : undefined,
  } as NavigatorScreenParams<TabParamList>);
}

export function flushPendingNotificationNavigation() {
  if (pendingZoneId !== undefined) {
    const zoneId = pendingZoneId;
    pendingZoneId = undefined;
    openZoneFromNotification(zoneId);
  }
}
