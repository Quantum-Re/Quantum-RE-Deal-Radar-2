import { Composition } from "remotion";
import { DealRadarIntro } from "./DealRadarIntro";

export const RemotionRoot: React.FC = () => (
  <Composition
    id="DealRadarIntro"
    component={DealRadarIntro}
    durationInFrames={90}
    fps={30}
    width={1080}
    height={1080}
    defaultProps={{ title: "Quantum Deal Radar" }}
  />
);
