import React from "react";
import { NavigationContainer, DarkTheme, DefaultTheme } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { Ionicons } from "@expo/vector-icons";
import { BlurView } from "expo-blur";
import { StyleSheet } from "react-native";

import { useAppTheme } from "@/theme/ThemeContext";
import HomeScreen from "@/screens/HomeScreen";
import MapScreen from "@/screens/MapScreen";
import StatisticsScreen from "@/screens/StatisticsScreen";
import NotificationsScreen from "@/screens/NotificationsScreen";
import InstructionsScreen from "@/screens/InstructionsScreen";
import SettingsScreen from "@/screens/SettingsScreen";
import StationDetailScreen from "@/screens/StationDetailScreen";
import { RootStackParamList, TabParamList } from "./types";
import { flushPendingNotificationNavigation, navigationRef } from "./navigationRef";

const Tab = createBottomTabNavigator<TabParamList>();
const Stack = createNativeStackNavigator<RootStackParamList>();

function Tabs() {
  const { colors, themeMode } = useAppTheme();

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: colors.textTertiary,
        tabBarStyle: {
          position: "absolute",
          borderTopWidth: 0,
          elevation: 0,
          height: 84,
          paddingTop: 8,
        },
        tabBarBackground: () => (
          <BlurView
            tint={themeMode === "dark" ? "dark" : "light"}
            intensity={80}
            style={StyleSheet.absoluteFill}
          />
        ),
        tabBarLabelStyle: { fontSize: 11, fontWeight: "600" },
        tabBarIcon: ({ color, size, focused }) => {
          const icons: Record<keyof TabParamList, keyof typeof Ionicons.glyphMap> = {
            Home: focused ? "home" : "home-outline",
            MapTab: focused ? "map" : "map-outline",
            Statistics: focused ? "bar-chart" : "bar-chart-outline",
            Notifications: focused ? "notifications" : "notifications-outline",
            Instructions: focused ? "document-text" : "document-text-outline",
            Settings: focused ? "settings" : "settings-outline",
          };
          return <Ionicons name={icons[route.name]} size={size - 2} color={color} />;
        },
      })}
    >
      <Tab.Screen name="Home" component={HomeScreen} options={{ title: "Главная" }} />
      <Tab.Screen name="MapTab" component={MapScreen} options={{ title: "Карта" }} />
      <Tab.Screen name="Statistics" component={StatisticsScreen} options={{ title: "Статистика" }} />
      <Tab.Screen name="Notifications" component={NotificationsScreen} options={{ title: "Уведомления" }} />
      <Tab.Screen name="Instructions" component={InstructionsScreen} options={{ title: "Инструкции" }} />
      <Tab.Screen name="Settings" component={SettingsScreen} options={{ title: "Настройки" }} />
    </Tab.Navigator>
  );
}

export default function RootNavigator() {
  const { colors, themeMode } = useAppTheme();

  const navTheme = {
    ...(themeMode === "dark" ? DarkTheme : DefaultTheme),
    colors: {
      ...(themeMode === "dark" ? DarkTheme.colors : DefaultTheme.colors),
      background: colors.background,
      card: colors.cardSolid,
      text: colors.text,
      border: colors.border,
      primary: colors.accent,
    },
  };

  return (
    <NavigationContainer ref={navigationRef} theme={navTheme} onReady={flushPendingNotificationNavigation}>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name="Tabs" component={Tabs} />
        <Stack.Screen
          name="StationDetail"
          component={StationDetailScreen}
          options={{
            headerShown: true,
            headerTitle: "",
            headerTransparent: true,
            headerTintColor: colors.text,
          }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
