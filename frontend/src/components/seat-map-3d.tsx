"use client";

import { useEffect, useRef } from "react";
import * as THREE from "three";
import { useBookingStore } from "@/store/booking-store";
import { Card } from "@/components/ui/card";

const colors = {
  AVAILABLE: 0x22c55e,
  HELD: 0xeab308,
  BOOKED: 0xef4444
};

export function SeatMap3D() {
  const mountRef = useRef<HTMLDivElement | null>(null);
  const seats = useBookingStore((s) => s.seats);
  const setSelectedSeat = useBookingStore((s) => s.setSelectedSeat);

  useEffect(() => {
    if (!mountRef.current) return;

    const width = mountRef.current.clientWidth;
    const height = 360;

    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x09090b);

    const camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 1000);
    camera.position.set(0, 12, 24);

    const renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setSize(width, height);
    mountRef.current.appendChild(renderer.domElement);

    const raycaster = new THREE.Raycaster();
    const pointer = new THREE.Vector2();

    const ambient = new THREE.AmbientLight(0xffffff, 1.2);
    scene.add(ambient);

    const meshes: Array<{ mesh: THREE.Mesh; seatId: number; seatNumber: string }> = [];

    seats.forEach((seat, idx) => {
      const geometry = new THREE.BoxGeometry(0.9, 0.9, 0.9);
      const material = new THREE.MeshStandardMaterial({ color: colors[seat.status] });
      const cube = new THREE.Mesh(geometry, material);
      const row = Math.floor(idx / 10);
      const col = idx % 10;
      cube.position.set(col - 4.5, 4 - row * 1.4, 0);
      scene.add(cube);
      meshes.push({ mesh: cube, seatId: seat.id, seatNumber: seat.seatNumber });
    });

    const onClick = (event: MouseEvent) => {
      const rect = renderer.domElement.getBoundingClientRect();
      pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
      pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
      raycaster.setFromCamera(pointer, camera);
      const intersects = raycaster.intersectObjects(meshes.map((m) => m.mesh));
      if (!intersects.length) return;
      const selected = meshes.find((m) => m.mesh === intersects[0].object);
      if (selected) {
        setSelectedSeat(selected.seatId, selected.seatNumber);
      }
    };

    renderer.domElement.addEventListener("click", onClick);

    const animate = () => {
      renderer.render(scene, camera);
      requestAnimationFrame(animate);
    };
    animate();

    return () => {
      renderer.domElement.removeEventListener("click", onClick);
      renderer.dispose();
      mountRef.current?.removeChild(renderer.domElement);
    };
  }, [seats, setSelectedSeat]);

  return (
    <Card>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-semibold">3D Seat Selection</h2>
        <div className="flex gap-2 text-xs">
          <span className="rounded bg-emerald-600/20 px-2 py-1">Available</span>
          <span className="rounded bg-yellow-500/20 px-2 py-1">Held</span>
          <span className="rounded bg-red-600/20 px-2 py-1">Booked</span>
        </div>
      </div>
      <div ref={mountRef} className="h-[360px] w-full rounded-lg border border-border" />
    </Card>
  );
}
