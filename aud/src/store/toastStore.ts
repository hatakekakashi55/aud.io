import { create } from 'zustand'

export type ToastIcon = 'check' | 'heart' | 'queue' | 'link' | 'trash' | 'info' | 'plus' | 'user'

export interface Toast {
  id: number
  title: string
  description?: string
  icon: ToastIcon
}

interface ToastState {
  toasts: Toast[]
  push: (toast: Omit<Toast, 'id'>) => void
  dismiss: (id: number) => void
}

let nextId = 1

export const useToastStore = create<ToastState>((set, get) => ({
  toasts: [],

  push: (toast) => {
    const id = nextId++
    set({ toasts: [...get().toasts.slice(-2), { ...toast, id }] })
    window.setTimeout(() => get().dismiss(id), 4200)
  },

  dismiss: (id) => set({ toasts: get().toasts.filter((t) => t.id !== id) }),
}))
