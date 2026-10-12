import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";

export const DealRadarIntro: React.FC<{ title: string }> = ({ title }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const scale = spring({ frame, fps, config: { damping: 12 } });
  const opacity = interpolate(frame, [0, 20], [0, 1], { extrapolateRight: "clamp" });

  return (
    <AbsoluteFill style={{ backgroundColor: "#0b1220", justifyContent: "center", alignItems: "center" }}>
      <h1 style={{ color: "white", fontFamily: "sans-serif", fontSize: 90, transform: `scale(${scale})`, opacity }}>
        {title}
      </h1>
    </AbsoluteFill>
  );
};
