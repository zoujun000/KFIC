import { ref, watch } from 'vue'

/**
 * 防抖 composable — 输入值变化后延迟指定毫秒才更新
 * @param {import('vue').Ref} source 源响应式值
 * @param {number} delay 延迟毫秒数（默认 300）
 * @returns {import('vue').Ref} 防抖后的值
 */
export function useDebounce(source, delay = 300) {
  const debounced = ref(source.value)
  let timer = null

  watch(source, (val) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      debounced.value = val
    }, delay)
  })

  return debounced
}
