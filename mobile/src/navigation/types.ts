import { NavigatorScreenParams } from "@react-navigation/native";

export type RootStackParamList = {
  Tabs: NavigatorScreenParams<TabParamList> | undefined;
  StationDetail: { stationId: string; stationName?: string };
};

export type TabParamList = {
  Home: undefined;
  MapTab: { zoneId?: string } | undefined;
  Statistics: undefined;
  Notifications: undefined;
  Instructions: undefined;
  Settings: undefined;
};
