<template>
  <div style="position:relative;width:100%;height:calc(100vh - 120px)">
    <div ref="globeContainer" style="width:100%;height:100%" />

    <!-- Camera info panel -->
    <transition name="el-fade-in">
      <el-card v-if="selected" style="position:absolute;top:16px;right:16px;width:340px;z-index:10" shadow="always">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span style="font-weight:600">{{ selected.cameraId }}</span>
            <el-icon style="cursor:pointer" @click="selected = null"><Close /></el-icon>
          </div>
        </template>
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="位置">{{ selected.location }}</el-descriptions-item>
          <el-descriptions-item label="事件类型">
            <el-tag :type="eventTagType(selected.eventType)" size="small">
              {{ EVENT_TYPE_LABEL[selected.eventType] || selected.eventType }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="置信度">{{ (selected.confidence * 100).toFixed(1) }}%</el-descriptions-item>
          <el-descriptions-item label="工单数">{{ selected.ticketCount }}</el-descriptions-item>
        </el-descriptions>
        <el-image
          v-if="selected.imageUrl"
          :src="selected.imageUrl"
          fit="cover"
          style="width:100%;height:160px;margin-top:12px;border-radius:4px"
        />
      </el-card>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import Globe from 'globe.gl'
import { listTickets } from '@shared/api/tickets'
import { EVENT_TYPE_LABEL } from '@shared/utils/constants'
import type { Ticket } from '@shared/types'
import { Close } from '@element-plus/icons-vue'

interface CameraPoint {
  lat: number
  lng: number
  cameraId: string
  location: string
  eventType: string
  confidence: number
  ticketCount: number
  imageUrl?: string
  color: string
}

const globeContainer = ref<HTMLElement>()
const selected = ref<CameraPoint | null>(null)

let globe: ReturnType<typeof Globe> | null = null
let resizeObserver: ResizeObserver | null = null
let autoRotateTimer: ReturnType<typeof setTimeout> | null = null

const EVENT_COLORS: Record<string, string> = {
  smoke_flame: '#f56c6c',
  parking_violation: '#e6a23c',
  common_space_utilization: '#409eff',
}

function eventTagType(eventType: string) {
  const map: Record<string, string> = {
    smoke_flame: 'danger',
    parking_violation: 'warning',
    common_space_utilization: '',
  }
  return map[eventType] ?? 'info'
}

function parseLatLng(latLng: string): [number, number] | null {
  const parts = latLng.split(',').map(Number)
  if (parts.length === 2 && !isNaN(parts[0]) && !isNaN(parts[1])) return [parts[0], parts[1]]
  return null
}

function buildPoints(tickets: Ticket[]): CameraPoint[] {
  const grouped = new Map<string, Ticket[]>()
  for (const t of tickets) {
    if (!t.latLng || !t.cameraId) continue
    const arr = grouped.get(t.cameraId) || []
    arr.push(t)
    grouped.set(t.cameraId, arr)
  }

  const points: CameraPoint[] = []
  for (const [cameraId, tks] of grouped) {
    // sort by eventTimestamp desc, take latest
    tks.sort((a, b) => b.eventTimestamp - a.eventTimestamp)
    const latest = tks[0]
    const coords = parseLatLng(latest.latLng!)
    if (!coords) continue
    points.push({
      lat: coords[0],
      lng: coords[1],
      cameraId,
      location: latest.location || '未知位置',
      eventType: latest.eventType,
      confidence: latest.confidence,
      ticketCount: tks.length,
      imageUrl: latest.imageUrl,
      color: EVENT_COLORS[latest.eventType] || '#67c23a',
    })
  }
  return points
}

function startAutoRotate() {
  if (!globe) return
  const controls = globe.controls() as any
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.5
}

function pauseAutoRotate() {
  if (!globe) return
  const controls = globe.controls() as any
  controls.autoRotate = false
  if (autoRotateTimer) clearTimeout(autoRotateTimer)
  autoRotateTimer = setTimeout(startAutoRotate, 3000)
}

async function init() {
  if (!globeContainer.value) return

  const { data } = await listTickets({ size: 500 })
  const points = buildPoints(data.content)

  globe = Globe()(globeContainer.value)
    .globeImageUrl('//unpkg.com/three-globe/example/img/earth-blue-marble.jpg')
    .bumpImageUrl('//unpkg.com/three-globe/example/img/earth-topology.png')
    .backgroundImageUrl('//unpkg.com/three-globe/example/img/night-sky.png')
    .pointsData(points)
    .pointLat('lat')
    .pointLng('lng')
    .pointColor('color')
    .pointAltitude(0.01)
    .pointRadius(0.4)
    .pointLabel((d: any) => `<b>${d.cameraId}</b><br/>${d.location}`)
    .onPointClick((point: any) => {
      selected.value = point as CameraPoint
      pauseAutoRotate()
    })

  // size to container
  const { clientWidth, clientHeight } = globeContainer.value
  globe.width(clientWidth).height(clientHeight)

  startAutoRotate()

  // pause on user interaction
  const controls = globe.controls() as any
  controls.addEventListener('start', pauseAutoRotate)

  // responsive resize
  resizeObserver = new ResizeObserver(([entry]) => {
    if (globe) {
      globe.width(entry.contentRect.width).height(entry.contentRect.height)
    }
  })
  resizeObserver.observe(globeContainer.value)
}

onMounted(init)

onUnmounted(() => {
  if (autoRotateTimer) clearTimeout(autoRotateTimer)
  if (resizeObserver) resizeObserver.disconnect()
  if (globe) {
    const el = globe.renderer().domElement
    el.parentNode?.removeChild(el)
    globe.scene().clear()
    globe.renderer().dispose()
    globe = null
  }
})
</script>
