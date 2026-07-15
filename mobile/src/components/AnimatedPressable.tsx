import React, { useRef } from "react";
import { Animated, Pressable, PressableProps, StyleProp, ViewStyle } from "react-native";

interface Props extends Omit<PressableProps, "style"> {
  children: React.ReactNode;
  scaleTo?: number;
  style?: StyleProp<ViewStyle>;
}

// Uses React Native's built-in Animated API (no react-native-reanimated /
// react-native-worklets needed) so this works reliably in Expo Go on any
// SDK version without native-module version mismatches.
export const AnimatedPressable: React.FC<Props> = ({
  children,
  scaleTo = 0.96,
  style,
  onPressIn,
  onPressOut,
  ...rest
}) => {
  const scale = useRef(new Animated.Value(1)).current;

  const animateTo = (toValue: number, duration: number) => {
    Animated.timing(scale, {
      toValue,
      duration,
      useNativeDriver: true,
    }).start();
  };

  return (
    <Animated.View style={[style, { transform: [{ scale }] }]}>
      <Pressable
        onPressIn={(e) => {
          animateTo(scaleTo, 100);
          onPressIn?.(e);
        }}
        onPressOut={(e) => {
          animateTo(1, 150);
          onPressOut?.(e);
        }}
        {...rest}
      >
        {children}
      </Pressable>
    </Animated.View>
  );
};
