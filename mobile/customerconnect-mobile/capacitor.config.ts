import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.llenroctech.customerconnect',
  appName: 'Llenroc Enterprise Operations',
  webDir: 'dist/customerconnect-mobile/browser',
  server: { androidScheme: 'https' },
  plugins: {
    Keyboard: { resize: 'body' },
  },
};

export default config;
