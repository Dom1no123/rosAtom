import { Platform } from "react-native";
import * as Notifications from "expo-notifications";

import { Alert } from "@/types";

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldPlaySound: true,
    shouldSetBadge: true,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
});

export async function prepareLocalNotifications(): Promise<boolean> {
  if (Platform.OS === "web") return false;
  if (Platform.OS === "android") {
    await Notifications.setNotificationChannelAsync("radiation-alerts", {
      name: "Радиационные тревоги",
      importance: Notifications.AndroidImportance.MAX,
      vibrationPattern: [0, 300, 200, 300],
      sound: "default",
    });
  }
  const current = await Notifications.getPermissionsAsync();
  const result = current.granted ? current : await Notifications.requestPermissionsAsync();
  return result.granted;
}

export async function showAlertNotification(alert: Alert): Promise<void> {
  if (!(await prepareLocalNotifications())) return;
  await Notifications.scheduleNotificationAsync({
    content: {
      title: "Критический уровень радиации",
      body: `Зона «${alert.targetName}». Откройте приложение и следуйте инструкции.`,
      sound: "default",
      data: { zoneId: alert.targetId },
    },
    trigger: null,
  });
}

export async function scheduleAutomaticNotification(timestamp: number, zoneId?: string): Promise<void> {
  if (!(await prepareLocalNotifications())) return;
  await Notifications.cancelAllScheduledNotificationsAsync();
  await Notifications.scheduleNotificationAsync({
    content: {
      title: "Автоматическая симуляция ЧП",
      body: "В одной из зон обнаружен критический уровень. Откройте приложение для подробностей.",
      sound: "default",
      data: { zoneId },
    },
    trigger: { type: Notifications.SchedulableTriggerInputTypes.DATE, date: new Date(timestamp), channelId: "radiation-alerts" },
  });
}

export async function cancelAutomaticNotifications(): Promise<void> {
  if (Platform.OS !== "web") await Notifications.cancelAllScheduledNotificationsAsync();
}

export const addNotificationResponseListener = Notifications.addNotificationResponseReceivedListener;
