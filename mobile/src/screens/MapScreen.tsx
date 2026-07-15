import React, { useEffect, useState } from "react";
import { Modal, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";

import { useAppTheme } from "@/theme/ThemeContext";
import { useZonesContext } from "@/context/ZonesContext";
import { Zone } from "@/types";
import { getStatusColor, getStatusLabel } from "@/utils/radiation";
import { formatDateTime } from "@/utils/date";
import { RadiationBadge } from "@/components/RadiationBadge";
import { GlassCard } from "@/components/GlassCard";
import { AnimatedPressable } from "@/components/AnimatedPressable";
import { ZoneMapView } from "@/components/ZoneMapView";
import { RouteProp, useRoute } from "@react-navigation/native";
import { TabParamList } from "@/navigation/types";

export default function MapScreen() {
  const { colors } = useAppTheme();
  const { zones, tick } = useZonesContext();
  const route = useRoute<RouteProp<TabParamList, "MapTab">>();
  const [selected, setSelected] = useState<Zone | null>(null);

  useEffect(() => {
    const requested = zones.find((zone) => zone.id === route.params?.zoneId);
    if (requested) setSelected(requested);
  }, [route.params?.zoneId, zones]);

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.text }]}>Зоны радиации</Text>
        <View style={[styles.sourceBadge, { backgroundColor: "#30D15833" }]}>
          <View
            style={[
              styles.sourceDot,
              { backgroundColor: colors.normal },
            ]}
          />
          <Text style={[styles.sourceText, { color: colors.normal }]}>
            Автономная симуляция
          </Text>
        </View>
      </View>

      <View style={{ flex: 1, marginTop: 100 }}>
        <ZoneMapView zones={zones} tick={tick} onZonePress={setSelected} />
      </View>

      <Modal visible={!!selected} transparent animationType="fade" onRequestClose={() => setSelected(null)}>
        <View style={styles.modalBackdrop}>
          <View style={styles.modalWrap}>
            {selected && (
              <GlassCard radius={28}>
                <View style={styles.modalHeaderRow}>
                  <View style={{ flex: 1 }}>
                    <Text style={[styles.zoneName, { color: colors.text }]}>Зона «{selected.name}»</Text>
                  </View>
                  <AnimatedPressable onPress={() => setSelected(null)}>
                    <Ionicons name="close-circle" size={26} color={colors.textTertiary} />
                  </AnimatedPressable>
                </View>

                <View style={[styles.divider, { backgroundColor: colors.border }]} />

                <View style={styles.infoRow}>
                  <Ionicons name="time-outline" size={16} color={colors.textTertiary} />
                  <Text style={[styles.infoText, { color: colors.textSecondary }]}>
                    {formatDateTime(selected.updatedAt)}
                  </Text>
                </View>

                <View style={styles.modalBottomRow}>
                  <Text style={[styles.levelValue, { color: colors.text }]}>{selected.level.toFixed(2)} мкЗв/ч</Text>
                  <RadiationBadge status={selected.status} />
                </View>
                <Text style={[styles.statusHint, { color: colors.textSecondary }]}>
                  {getStatusLabel(selected.status)}
                </Text>
              </GlassCard>
            )}
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  header: { position: "absolute", top: 56, left: 20, right: 20, zIndex: 10 },
  title: { fontSize: 22, fontWeight: "800" },
  sourceBadge: {
    flexDirection: "row",
    alignItems: "center",
    alignSelf: "flex-start",
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 100,
    marginTop: 8,
  },
  sourceDot: { width: 7, height: 7, borderRadius: 4, marginRight: 6 },
  sourceText: { fontSize: 11, fontWeight: "700" },
  modalBackdrop: { flex: 1, backgroundColor: "rgba(0,0,0,0.4)", justifyContent: "flex-end" },
  modalWrap: { padding: 16, paddingBottom: 32 },
  modalHeaderRow: { flexDirection: "row", alignItems: "flex-start" },
  zoneName: { fontSize: 20, fontWeight: "800" },
  divider: { height: 1, marginVertical: 14 },
  infoRow: { flexDirection: "row", alignItems: "center", marginBottom: 8 },
  infoText: { fontSize: 13, marginLeft: 6 },
  modalBottomRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginTop: 8,
  },
  levelValue: { fontSize: 22, fontWeight: "800" },
  statusHint: { fontSize: 12, marginTop: 6 },
});
