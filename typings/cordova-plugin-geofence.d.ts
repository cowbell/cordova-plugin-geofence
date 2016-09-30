interface TransitionType {
  ENTER: number;
  EXIT: number;
  BOTH: number;
}

interface Window {
  geofence: GeofencePlugin;
  TransitionType: TransitionType;
}

interface GeofencePlugin {
  initialize(
    successCallback?: (result: any) => void,
    errorCallback?: (error: string) => void
  ): Promise<any>;

  addOrUpdate(
    geofence: Geofence | Geofence[],
    successCallback?: (result: any) => void,
    errorCallback?: (error: string) => void
  ): Promise<any>;

  remove(
    id: number | number[],
    successCallback?: (result: any) => void,
    errorCallback?: (error: string) => void
  ): Promise<any>;

  removeAll(
    successCallback?: (result: any) => void,
    errorCallback?: (error: string) => void
  ): Promise<any>;

  getWatched(
    successCallback?: (result: any) => void,
    errorCallback?: (error: string) => void
  ): Promise<string>;

  onTransitionReceived: (geofences: Geofence[]) => void;
  
  onNotificationClicked: (notificationData: Object) => void;
}

interface Geofence {
  id: string;
  latitude: number;
  longitude: number;
  radius: number;
  transitionType: number;
  notification?: Notification;
}

interface Notification {
  id?: number;
  title?: string;
  text: string;
  smallIcon?: string;
  icon?: string;
  openAppOnClick?: boolean;
  vibration?: number[];
  data?: Object;
}
