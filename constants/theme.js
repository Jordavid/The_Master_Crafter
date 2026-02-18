// Paleta de colores según el documento de diseño
export const COLORS = {
  background: '#0A1428',
  accentGold: '#C89B3C',
  magicBlue: '#0BC6E3',
  progressGreen: '#00BFA5',
  panelDark: '#1A2332',
  textMain: '#F0E6D2',
  itemBorder: '#463714',
  successGreen: '#00FF88',
  errorRed: '#FF4444',
};

// Configuración del juego
export const GAME_CONFIG = {
  timePerQuestion: 30, // segundos
  pointsPerCorrect: 100,
  itemsInCircle: 10,
  circleRadius: 250,
  centralItemSize: 120,
  peripheralItemSize: 64,
  maxSlots: 2, // Cantidad de items que componen la receta
};

// API Configuration
export const API_CONFIG = {
  baseURL: 'http://localhost:8080/api',
  endpoints: {
    question: '/game/question',
    validate: '/game/validate',
  },
};