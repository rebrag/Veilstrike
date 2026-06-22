// ============================================================
// Veilstrike — Theme tokens for JS / Phaser
// Direction A · "Twilight Tactician" · v1.0
// Phaser expects numeric hex (0x...). Strings provided for CSS-in-JS.
// ============================================================

export const VS = {
  // Core surfaces
  abyss:   0x0E1726,
  slate:   0x1C2C42,
  surface: 0x243B57,
  line:    0x3A5570,

  // Accent
  teal:    0x2BB6A3,
  glow:    0x7FE0D4,

  // Text
  mist:    0xE8EDF2,
  haze:    0x8AA0B5,
  ink:     0x0A111C, // sprite outline / text on light accent

  // Classes
  warrior: 0xC0894E, // Ironwood
  rogue:   0xE6D24A, // Sulfur
  mage:    0x8AA6EC, // Arcane
  priest:  0xCDD8E4, // Pale Halo

  // Semantic
  info:    0x4A9BE0,
  success: 0x2BB6A3,
  warning: 0xE0A93B,
  danger:  0xD9534F,
};

// Hex strings (for CSS-in-JS, DOM overlays, gradients)
export const VS_HEX = {
  abyss: '#0E1726', slate: '#1C2C42', surface: '#243B57', line: '#3A5570',
  teal: '#2BB6A3', glow: '#7FE0D4', mist: '#E8EDF2', haze: '#8AA0B5', ink: '#0A111C',
  warrior: '#C0894E', rogue: '#E6D24A', mage: '#8AA6EC', priest: '#CDD8E4',
  info: '#4A9BE0', success: '#2BB6A3', warning: '#E0A93B', danger: '#D9534F',
};

// Class lookup keyed by class id used in game logic
export const CLASS_COLORS = {
  WARRIOR: VS.warrior,
  ROGUE:   VS.rogue,
  MAGE:    VS.mage,
  PRIEST:  VS.priest,
};

// Fog of war: alpha for the Abyss overlay on explored-but-unseen tiles
export const FOG = {
  exploredAlpha: 0.78,
  ditherEdgePx: 2,
};

// Recommended Phaser game config fragment for crisp pixel art
export const PHASER_RENDER = {
  pixelArt: true,
  roundPixels: true,
  backgroundColor: '#0E1726',
};
