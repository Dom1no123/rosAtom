import React from "react";
import { ScrollView, StyleSheet, Switch, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";

import { useAppTheme } from "@/theme/ThemeContext";
import { GlassCard } from "@/components/GlassCard";
import { AnimatedPressable } from "@/components/AnimatedPressable";
import { Language, SimulationMode, ThemeMode } from "@/types";
import { useRadiation } from "@/context/RadiationContext";

const languages: { code: Language; label: string }[] = [
  { code: "ru", label: "Русский" },
  { code: "kk", label: "Қазақша" },
  { code: "en", label: "English" },
];

const intervals = [30, 60, 300, 600];

export default function SettingsScreen() {
  const { snapshot, setSimulationMode: setEngineMode } = useRadiation();
  const {
    colors,
    themeMode,
    setThemeMode,
    language,
    setLanguage,
    notificationsEnabled,
    setNotificationsEnabled,
    refreshIntervalSec,
    setRefreshIntervalSec,
  } = useAppTheme();

  const Row: React.FC<{ icon: keyof typeof Ionicons.glyphMap; label: string; children: React.ReactNode }> = ({
    icon,
    label,
    children,
  }) => (
    <View style={styles.row}>
      <View style={styles.rowLeft}>
        <Ionicons name={icon} size={18} color={colors.accent} />
        <Text style={[styles.rowLabel, { color: colors.text }]}>{label}</Text>
      </View>
      {children}
    </View>
  );

  const SegmentButton = ({
    active,
    label,
    onPress,
  }: {
    active: boolean;
    label: string;
    onPress: () => void;
  }) => (
    <AnimatedPressable
      onPress={onPress}
      style={[
        styles.segment,
        {
          backgroundColor: active ? colors.accent : "transparent",
          borderColor: colors.border,
        },
      ]}
    >
      <Text style={{ color: active ? "#fff" : colors.textSecondary, fontWeight: "600", fontSize: 13 }}>
        {label}
      </Text>
    </AnimatedPressable>
  );

  return (
    <ScrollView style={{ backgroundColor: colors.background }} contentContainerStyle={{ paddingBottom: 40 }}>
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.text }]}>Настройки</Text>
      </View>

      <View style={styles.section}>
        <GlassCard radius={24}>
          <Row icon="contrast-outline" label="Тема оформления">
            <View style={styles.segmentGroup}>
              <SegmentButton
                active={themeMode === "light"}
                label="Светлая"
                onPress={() => setThemeMode("light" as ThemeMode)}
              />
              <SegmentButton
                active={themeMode === "dark"}
                label="Тёмная"
                onPress={() => setThemeMode("dark" as ThemeMode)}
              />
            </View>
          </Row>
        </GlassCard>
      </View>

      <View style={styles.section}>
        <GlassCard radius={24}>
          <Text style={[styles.groupTitle, { color: colors.textSecondary }]}>Язык интерфейса · скоро</Text>
          <View style={[styles.segmentGroup, { marginTop: 6 }]}>
            {languages.map((l) => (
              <SegmentButton
                key={l.code}
                active={language === l.code}
                label={l.label}
                onPress={() => l.code === "ru" && setLanguage(l.code)}
              />
            ))}
          </View>
        </GlassCard>
      </View>

      <View style={styles.section}>
        <GlassCard radius={24}>
          <Row icon="notifications-outline" label="Локальные уведомления">
            <Switch
              value={notificationsEnabled}
              onValueChange={setNotificationsEnabled}
              trackColor={{ true: colors.accent, false: colors.border }}
            />
          </Row>
          <Text style={[styles.helper, { color: colors.textTertiary }]}>Работают без сервера. Разрешение Android будет запрошено при первом событии.</Text>
        </GlassCard>
      </View>

      <View style={styles.section}>
        <GlassCard radius={24}>
          <Text style={[styles.groupTitle, { color: colors.textSecondary }]}>Режим аварийных событий</Text>
          <View style={[styles.segmentGroup, { marginTop: 6 }]}>
            {([
              ["manual", "Только вручную"],
              ["automatic", "Автосимуляция"],
            ] as [SimulationMode, string][]).map(([mode, label]) => (
              <SegmentButton
                key={mode}
                active={snapshot.simulationMode === mode}
                label={label}
                onPress={() => {
                  setEngineMode(mode);
                }}
              />
            ))}
          </View>
          <Text style={[styles.helper, { color: colors.textTertiary }]}>В автоматическом режиме событие возникает через 2–5 минут. Одновременно активна только одна зона.</Text>
        </GlassCard>
      </View>

      <View style={styles.section}>
        <GlassCard radius={24}>
          <Text style={[styles.groupTitle, { color: colors.textSecondary }]}>
            Интервал обновления данных
          </Text>
          <View style={[styles.segmentGroup, { marginTop: 6, flexWrap: "wrap" }]}>
            {intervals.map((sec) => (
              <SegmentButton
                key={sec}
                active={refreshIntervalSec === sec}
                label={sec < 60 ? `${sec} сек` : `${sec / 60} мин`}
                onPress={() => setRefreshIntervalSec(sec)}
              />
            ))}
          </View>
        </GlassCard>
      </View>

      <View style={styles.section}>
        <Text style={[styles.footerNote, { color: colors.textTertiary }]}>
          Radiation Monitor · v1.0.0 · Хакатон-проект
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  header: { paddingTop: 60, paddingHorizontal: 20, paddingBottom: 10 },
  title: { fontSize: 28, fontWeight: "800" },
  section: { paddingHorizontal: 20, marginTop: 16 },
  row: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  rowLeft: { flexDirection: "row", alignItems: "center" },
  rowLabel: { fontSize: 15, fontWeight: "600", marginLeft: 10 },
  groupTitle: { fontSize: 13, fontWeight: "600" },
  segmentGroup: { flexDirection: "row" },
  segment: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 100,
    borderWidth: 1,
    marginRight: 8,
    marginTop: 6,
  },
  footerNote: { textAlign: "center", fontSize: 12 },
  helper: { fontSize: 11, lineHeight: 16, marginTop: 10 },
});
