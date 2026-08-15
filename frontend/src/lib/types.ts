export type SeatStatus = "AVAILABLE" | "HELD" | "BOOKED";

export type QueueStatus = {
  userId: string;
  position: number;
  estimatedWaitSeconds: number;
  admitted: boolean;
  queueToken?: string | null;
  tokenExpiresInSeconds?: number;
};

export type HoldSeatResponse = {
  seatId: number;
  status: "HELD";
  holdExpiresAt: string;
  message: string;
};

export type BookingResponse = {
  bookingId: number;
  status: string;
  message: string;
};

export type SeatView = {
  id: number;
  seatNumber: string;
  status: SeatStatus;
};
