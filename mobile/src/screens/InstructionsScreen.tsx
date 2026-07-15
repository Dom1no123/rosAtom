import React, { useState } from "react";
import { ScrollView, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";

import { AnimatedPressable } from "@/components/AnimatedPressable";
import { GlassCard } from "@/components/GlassCard";
import { useZonesContext } from "@/context/ZonesContext";
import { NORMAL_INSTRUCTIONS } from "@/data/instructions";
import { useAppTheme } from "@/theme/ThemeContext";

export default function InstructionsScreen() {
  const { colors } = useAppTheme();
  const { triggerTestAlert } = useZonesContext();
  const [triggering, setTriggering] = useState(false);

  const handleTestAlert = async () => {
    setTriggering(true);
    try {
      await triggerTestAlert();
    } finally {
      setTriggering(false);
    }
  };

  return (
    <ScrollView style={{ backgroundColor: colors.background }} contentContainerStyle={{ paddingBottom: 40 }}>
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.text }]}>Инструкции</Text>
      </View>

      <View style={styles.section}>
        <GlassCard radius={24}>
          <View style={styles.rowHeader}>
            <Ionicons name="shield-checkmark-outline" size={20} color={colors.accent} />
            <Text style={[styles.cardTitle, { color: colors.text }]}>Общие рекомендации по безопасности</Text>
          </View>
          <Text style={[styles.bodyText, { color: colors.textSecondary }]}>{NORMAL_INSTRUCTIONS}</Text>
        </GlassCard>
      </View>

      <View style={styles.section}>
        <GlassCard radius={24} style={{ borderColor: `${colors.critical}55` }}>
          <View style={styles.rowHeader}>
            <Ionicons name="warning-outline" size={20} color={colors.critical} />
            <Text style={[styles.cardTitle, { color: colors.text }]}>Тестирование ЧП</Text>
          </View>
          <Text style={[styles.bodyText, { color: colors.textSecondary }]}>
            Кнопка локально переводит случайную зону в критическое состояние и открывает экстренную
            инструкцию. Интернет и запущенный сервер не требуются. Одновременно активна только одна
            тревога, через 60 секунд зона автоматически восстановится.
          </Text>
          <AnimatedPressable
            style={[styles.alertBtn, { backgroundColor: colors.critical, opacity: triggering ? 0.6 : 1 }]}
            onPress={handleTestAlert}
            disabled={triggering}
          >
            <Ionicons name="alert-circle" size={18} color="#fff" />
            <Text style={styles.alertBtnText}>{triggering ? "Запуск…" : "Тестовое ЧП (критическая зона)"}</Text>
          </AnimatedPressable>
        </GlassCard>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  header: { paddingTop: 60, paddingHorizontal: 20, paddingBottom: 10 },
  title: { fontSize: 28, fontWeight: "800" },
  section: { paddingHorizontal: 20, marginTop: 16 },
  rowHeader: { flexDirection: "row", alignItems: "center", marginBottom: 12 },
  cardTitle: { fontSize: 16, fontWeight: "700", marginLeft: 8, flex: 1 },
  bodyText: { fontSize: 13, lineHeight: 20 },
  alertBtn: { flexDirection: "row", alignItems: "center", justifyContent: "center", paddingVertical: 14, borderRadius: 100, marginTop: 16 },
  alertBtnText: { color: "#fff", fontWeight: "800", fontSize: 14, marginLeft: 8 },
});
