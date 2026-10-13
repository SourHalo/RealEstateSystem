import { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';

export default function PropertyCanvas({ propertyType, propertyId, address = '', theme = 'dark' }) {
  const mountRef = useRef(null);
  const [lightingMode, setLightingMode] = useState('night');

  useEffect(() => {
    const container = mountRef.current;
    if (!container) return;

    const width = container.clientWidth || 700;
    const height = container.clientHeight || 420;

    // Deterministic pseudo-random seed based on property ID
    const id = Number(propertyId) || 101;
    const hash = (n) => Math.abs(Math.sin(id * 997 + n * 17) * 10000) % 1;

    // Dynamic floor count based on address/flat number or ID
    let floorCount = 3;
    const flatMatch = address.match(/(?:Flat|Office|Shop|House)\s*(\d+)/i);
    if (flatMatch) {
      const num = parseInt(flatMatch[1], 10);
      floorCount = Math.max(2, Math.min(7, Math.floor(num / 100) + 1));
    } else {
      floorCount = 2 + (id % 4);
    }
    if (propertyType === 'Commercial') floorCount = Math.max(5, floorCount + 2);
    if (propertyType === 'House') floorCount = 2;

    // Procedural color variations
    const colorThemes = [
      { wall: 0xf1f5f9, trim: 0x1e293b, glass: 0x93c5fd, pool: 0x06b6d4 }, // Modern Crisp White
      { wall: 0x334155, trim: 0x0f172a, glass: 0x67e8f9, pool: 0x0284c7 }, // Charcoal Slate
      { wall: 0xe2d9cc, trim: 0x785336, glass: 0xfde047, pool: 0x14b8a6 }, // Warm Sandstone
      { wall: 0x27272a, trim: 0xd4b47e, glass: 0x93c5fd, pool: 0x38bdf8 }, // Gold & Midnight
    ];
    const palette = colorThemes[id % colorThemes.length];

    // 1. Scene Setup
    const scene = new THREE.Scene();
    const isNight = lightingMode === 'night';
    scene.background = new THREE.Color(isNight ? (theme === 'dark' ? '#090d16' : '#141c2b') : '#e2e8f0');
    scene.fog = new THREE.FogExp2(scene.background, 0.028);

    // 2. Camera Setup (adjusted dynamically for building height)
    const buildingHeight = floorCount * 0.95;
    const camera = new THREE.PerspectiveCamera(40, width / height, 0.1, 100);
    camera.position.set(9.5, Math.max(5.5, buildingHeight * 1.1), 10.5);
    camera.lookAt(0, buildingHeight * 0.4, 0);

    // 3. Renderer
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = isNight ? 1.25 : 1.0;
    container.appendChild(renderer.domElement);

    // 4. Lighting Rig
    const ambientLight = new THREE.AmbientLight(
      isNight ? 0x2b3954 : 0xffffff,
      isNight ? 0.85 : 1.35
    );
    scene.add(ambientLight);

    const sunLight = new THREE.DirectionalLight(isNight ? 0x8ba4d8 : 0xfff6e6, isNight ? 1.1 : 2.7);
    sunLight.position.set(12, 22, 10);
    sunLight.castShadow = true;
    sunLight.shadow.mapSize.width = 2048;
    sunLight.shadow.mapSize.height = 2048;
    scene.add(sunLight);

    // 5. Materials
    const wallMat = new THREE.MeshStandardMaterial({ color: palette.wall, roughness: 0.4 });
    const trimMat = new THREE.MeshStandardMaterial({ color: palette.trim, roughness: 0.6 });
    const woodMat = new THREE.MeshStandardMaterial({ color: 0x8a5732, roughness: 0.7 });
    const glassMat = new THREE.MeshPhysicalMaterial({
      color: palette.glass,
      transmission: 0.88,
      opacity: 0.65,
      transparent: true,
      roughness: 0.08,
      reflectivity: 0.9,
    });
    const windowGlowMat = new THREE.MeshStandardMaterial({
      color: isNight ? 0xffbf47 : 0xfff3c4,
      emissive: isNight ? 0xff9500 : 0xffd166,
      emissiveIntensity: isNight ? 2.0 : 0.35,
    });
    const grassMat = new THREE.MeshStandardMaterial({
      color: isNight ? 0x1a3328 : 0x3d7455,
      roughness: 0.9,
    });
    const poolMat = new THREE.MeshStandardMaterial({
      color: palette.pool,
      roughness: 0.1,
      metalness: 0.2,
      transparent: true,
      opacity: 0.85,
    });

    const rootGroup = new THREE.Group();

    // 6. Ground Platform
    const ground = new THREE.Mesh(new THREE.BoxGeometry(11, 0.2, 9.5), grassMat);
    ground.receiveShadow = true;
    ground.position.y = -0.1;
    rootGroup.add(ground);

    // =========================================================================
    // CASE A: COMMERCIAL HIGH-RISE TOWER
    // =========================================================================
    if (propertyType === 'Commercial') {
      const tower = new THREE.Mesh(new THREE.BoxGeometry(4.0, buildingHeight, 3.8), trimMat);
      tower.position.set(-0.2, buildingHeight / 2, 0);
      tower.castShadow = true;
      rootGroup.add(tower);

      for (let f = 1; f <= floorCount; f++) {
        const curtain = new THREE.Mesh(new THREE.BoxGeometry(4.05, 0.55, 3.85), glassMat);
        curtain.position.set(-0.2, f * 0.9 - 0.25, 0);
        rootGroup.add(curtain);

        const interiorLight = new THREE.Mesh(new THREE.BoxGeometry(3.9, 0.45, 3.7), windowGlowMat);
        interiorLight.position.set(-0.2, f * 0.9 - 0.25, 0);
        rootGroup.add(interiorLight);
      }

      // Helipad on top
      const helipad = new THREE.Mesh(new THREE.CylinderGeometry(1.4, 1.4, 0.12, 24), wallMat);
      helipad.position.set(-0.2, buildingHeight + 0.06, 0);
      rootGroup.add(helipad);

      const mast = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.06, 1.6), trimMat);
      mast.position.set(-0.2, buildingHeight + 0.9, 0);
      rootGroup.add(mast);
    }

    // =========================================================================
    // CASE B: RESIDENTIAL LUXURY HOUSE / VILLA (With Backyard Pool & Patio)
    // =========================================================================
    else if (propertyType === 'House') {
      // Wood Deck + Pool
      const deck = new THREE.Mesh(new THREE.BoxGeometry(3.6, 0.22, 5.2), woodMat);
      deck.position.set(2.4, 0.02, 0.2);
      rootGroup.add(deck);

      const pool = new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.24, 3.8), poolMat);
      pool.position.set(2.4, 0.04, 0.2);
      rootGroup.add(pool);

      const poolLight = new THREE.PointLight(0x00e5ff, isNight ? 2.5 : 0.6, 6);
      poolLight.position.set(2.4, 0.2, 0.2);
      scene.add(poolLight);

      // 2-Floor Villa Architecture
      const groundFloor = new THREE.Mesh(new THREE.BoxGeometry(4.2, 1.3, 3.4), wallMat);
      groundFloor.position.set(-1.0, 0.65, 0);
      groundFloor.castShadow = true;
      rootGroup.add(groundFloor);

      const groundGlass = new THREE.Mesh(new THREE.BoxGeometry(3.6, 1.05, 0.1), glassMat);
      groundGlass.position.set(-1.0, 0.65, 1.72);
      rootGroup.add(groundGlass);

      const secondFloor = new THREE.Mesh(new THREE.BoxGeometry(3.6, 1.2, 3.0), trimMat);
      secondFloor.position.set(-0.6, 1.9, 0.4);
      secondFloor.castShadow = true;
      rootGroup.add(secondFloor);

      // Louver sunshades
      for (let i = 0; i < 5; i++) {
        const slat = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.1, 1.8), woodMat);
        slat.position.set(-2.0 + i * 0.35, 2.55, 1.1);
        rootGroup.add(slat);
      }
    }

    // =========================================================================
    // CASE C: APARTMENT COMPLEX (Tiered Floors Based on Flat Number)
    // =========================================================================
    else {
      // Main Core Building
      const apt = new THREE.Mesh(new THREE.BoxGeometry(3.8, buildingHeight, 3.4), wallMat);
      apt.position.set(-0.3, buildingHeight / 2, 0);
      apt.castShadow = true;
      rootGroup.add(apt);

      // Generate exact number of floors corresponding to unit height
      for (let f = 1; f <= floorCount; f++) {
        const floorY = f * 0.95;

        // Balcony slab
        const balconySlab = new THREE.Mesh(new THREE.BoxGeometry(3.4, 0.1, 1.1), trimMat);
        balconySlab.position.set(-0.3, floorY - 0.35, 1.8);
        rootGroup.add(balconySlab);

        // Glass railing
        const railing = new THREE.Mesh(new THREE.BoxGeometry(3.4, 0.4, 0.05), glassMat);
        railing.position.set(-0.3, floorY - 0.15, 2.33);
        rootGroup.add(railing);

        // Illuminated window interior behind the balcony
        const windowPane = new THREE.Mesh(new THREE.BoxGeometry(3.0, 0.65, 0.05), windowGlowMat);
        windowPane.position.set(-0.3, floorY, 1.72);
        rootGroup.add(windowPane);
      }

      // Rooftop Garden Deck
      const roofLounge = new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.7, 2.0), trimMat);
      roofLounge.position.set(-0.3, buildingHeight + 0.35, -0.3);
      rootGroup.add(roofLounge);
    }

    // 7. Garden Trees
    const addTree = (x, z) => {
      const trunk = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.1, 0.8), woodMat);
      trunk.position.set(x, 0.4, z);
      rootGroup.add(trunk);

      const crown = new THREE.Mesh(new THREE.DodecahedronGeometry(0.55), grassMat);
      crown.position.set(x, 1.0, z);
      crown.castShadow = true;
      rootGroup.add(crown);
    };
    addTree(-4.2, -2.8);
    addTree(4.2, -3.0);
    if (hash(1) > 0.5) addTree(-3.8, 2.8);

    scene.add(rootGroup);

    // 8. Mouse Controls (Rotate + Zoom)
    let isDragging = false;
    let prevMouse = { x: 0, y: 0 };

    const onMouseDown = (e) => {
      isDragging = true;
      prevMouse = { x: e.clientX, y: e.clientY };
    };

    const onMouseMove = (e) => {
      if (!isDragging) return;
      const dx = e.clientX - prevMouse.x;
      const dy = e.clientY - prevMouse.y;

      rootGroup.rotation.y += dx * 0.007;
      camera.position.y = Math.max(2.5, Math.min(13.0, camera.position.y - dy * 0.015));
      camera.lookAt(0, buildingHeight * 0.4, 0);

      prevMouse = { x: e.clientX, y: e.clientY };
    };

    const onMouseUp = () => { isDragging = false; };

    const onWheel = (e) => {
      e.preventDefault();
      camera.fov = Math.max(20, Math.min(65, camera.fov + e.deltaY * 0.005));
      camera.updateProjectionMatrix();
    };

    const dom = renderer.domElement;
    dom.addEventListener('mousedown', onMouseDown);
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    dom.addEventListener('wheel', onWheel, { passive: false });

    // 9. Render Loop
    let animId;
    const animate = () => {
      if (!isDragging) {
        rootGroup.rotation.y += 0.0025;
      }
      renderer.render(scene, camera);
      animId = requestAnimationFrame(animate);
    };
    animate();

    return () => {
      cancelAnimationFrame(animId);
      dom.removeEventListener('mousedown', onMouseDown);
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mouseup', onMouseUp);
      dom.removeEventListener('wheel', onWheel);
      if (container.contains(dom)) container.removeChild(dom);
      renderer.dispose();
    };
  }, [propertyType, propertyId, address, theme, lightingMode]);

  return (
    <div style={{ position: 'relative', width: '100%', height: '420px', borderRadius: '10px', overflow: 'hidden' }}>
      <div ref={mountRef} style={{ width: '100%', height: '100%', cursor: 'grab' }} />

      <div style={{
        position: 'absolute',
        top: '12px',
        right: '12px',
        display: 'flex',
        gap: '6px',
        background: 'rgba(15, 23, 42, 0.75)',
        backdropFilter: 'blur(8px)',
        padding: '5px 8px',
        borderRadius: '20px',
        border: '1px solid rgba(255,255,255,0.1)'
      }}>
        <button
          onClick={() => setLightingMode('day')}
          style={{
            background: lightingMode === 'day' ? '#d4b47e' : 'transparent',
            color: lightingMode === 'day' ? '#121517' : '#94a3b8',
            border: 'none',
            borderRadius: '14px',
            padding: '3px 10px',
            fontSize: '0.72rem',
            fontWeight: 600,
            cursor: 'pointer'
          }}
        >
          ☀️ Daytime
        </button>
        <button
          onClick={() => setLightingMode('night')}
          style={{
            background: lightingMode === 'night' ? '#d4b47e' : 'transparent',
            color: lightingMode === 'night' ? '#121517' : '#94a3b8',
            border: 'none',
            borderRadius: '14px',
            padding: '3px 10px',
            fontSize: '0.72rem',
            fontWeight: 600,
            cursor: 'pointer'
          }}
        >
          🌙 Night Lighting
        </button>
      </div>

      <div style={{
        position: 'absolute',
        bottom: '12px',
        left: '12px',
        fontSize: '0.72rem',
        padding: '4px 10px',
        borderRadius: '20px',
        background: 'rgba(0, 0, 0, 0.6)',
        color: '#d4b47e',
        pointerEvents: 'none'
      }}>
        ✦ <strong>{propertyType} #{propertyId}</strong> &bull; Drag to rotate &bull; Scroll to zoom
      </div>
    </div>
  );
}