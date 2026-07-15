import React from "react";
import { FlatList, RefreshControl, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";

import { useAppTheme } from "@/theme/ThemeContext";
import { useAlerts } from "@/hooks/useAlerts";
import { GlassCard } from "@/components/GlassCard";
import { getStatusColor, getStatusLabel } from "@/utils/radiation";
import { timeAgo } from "@/utils/date";
import { Alert } from "@/types";

export default function NotificationsScreen() {
  const { colors } = useAppTheme();
  const { alerts, loading, refresh } = useAlerts();

  const renderItem = ({ item }: { item: Alert }) => {
    const color = getStatusColor(item.status);
    return (
      <GlassCard
        radius={22}
        style={{ marginBottom: 12, borderColor: `${color}55` }}
      >
        <View style={styles.itemHeader}>
          <View style={[styles.iconWrap, { backgroundColor: `${color}22` }]}>
            <Ionicons name="warning" size={18} color={color} />
          </View>
          <View style={{ flex: 1, marginLeft: 10 }}>
            <Text style={[styles.itemTitle, { color: colors.text }]}>{item.targetName}</Text>
            <Text style={[styles.itemStatus, { color }]}>{getStatusLabel(item.status)}</Text>
          </View>
          <Text style={[styles.itemTime, { color: colors.textTertiary }]}>
            {timeAgo(item.createdAt)}
          </Text>
        </View>
        <Text style={[styles.itemMessage, { color: colors.textSecondary }]}>{item.message}</Text>
      </GlassCard>
    );
  };

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.text }]}>Уведомления</Text>
        <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
          {alerts.length ? `${alerts.length} событий в журнале` : "Событий пока нет"}
        </Text>
      </View>

      <FlatList
        data={alerts}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        contentContainerStyle={{ paddingHorizontal: 20, paddingBottom: 40 }}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={refresh} tintColor={colors.accent} />}
        ListEmptyComponent={
          !loading ? (
            <View style={styles.emptyWrap}>
              <Ionicons name="shield-checkmark-outline" size={40} color={colors.normal} />
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
                Журнал аварийных событий пуст
              </Text>
            </View>
          ) : null
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  header: { paddingTop: 60, paddingHorizontal: 20, paddingBottom: 16 },
  title: { fontSize: 28, fontWeight: "800" },
  subtitle: { fontSize: 13, marginTop: 4 },
  itemHeader: { flexDirection: "row", alignItems: "center" },
  iconWrap: { width: 36, height: 36, borderRadius: 12, alignItems: "center", justifyContent: "center" },
  itemTitle: { fontSize: 15, fontWeight: "700" },
  itemStatus: { fontSize: 12, fontWeight: "700", marginTop: 2 },
  itemTime: { fontSize: 11 },
  itemMessage: { fontSize: 13, lineHeight: 18, marginTop: 10 },
  emptyWrap: { alignItems: "center", marginTop: 80 },
  emptyText: { fontSize: 14, marginTop: 12, textAlign: "center" },
});
