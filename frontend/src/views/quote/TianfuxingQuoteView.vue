<template>
  <div class="tfx-page">
    <section class="product-tabs" role="tablist">
      <button v-for="tab in tabs" :key="tab.value" class="product-tab"
        :class="{ active: mode === tab.value }" type="button" @click="switchMode(tab.value)">
        <span class="tab-icon"><el-icon><component :is="tab.icon" /></el-icon></span>
        <span><strong>{{ tab.label }}</strong><small>{{ tab.description }}</small></span>
      </button>
    </section>

    <section class="quote-workbench">
      <div class="route-strip">
        <div class="route-point">
          <el-icon><Location /></el-icon>
          <span>转运仓</span>
          <strong>{{ form.reWarehouseCode || '未指定仓库' }}</strong>
        </div>
        <div class="route-divider" />
        <div class="route-point">
          <el-icon><Location /></el-icon>
          <span>目的地</span>
          <el-select v-model="form.destNo" filterable allow-create default-first-option class="route-select" placeholder="选择目的地">
            <el-option v-for="country in countries" :key="country.code" :label="country.label" :value="country.code" />
          </el-select>
        </div>
      </div>
      <div class="quick-bar">
        <div class="quick-cell cargo-cell"><el-icon><Box /></el-icon><span>货物包含</span><strong>{{ form.packing }} / {{ goodsTypeLabel }}</strong></div>
        <div class="quick-cell"><strong>1</strong><span>件</span></div>
        <div class="quick-cell unit-cell"><el-input-number v-model="form.weig" class="quick-number" :min="0.001" :precision="3" :step="0.001" :controls="false" /><span>KG</span></div>
        <div class="quick-cell volume-cell"><el-input-number v-model="volumeCbmInput" class="quick-number" :min="0.000001" :precision="6" :step="0.000001" :controls="false" /><span>CBM</span></div>
        <el-button class="more-button" plain @click="advancedVisible = !advancedVisible">{{ advancedVisible ? '收起条件' : '更多条件' }}</el-button>
        <el-button class="quote-button" type="primary" :loading="loading" :icon="Search" @click="submitQuote">查价</el-button>
      </div>
    </section>

    <section v-show="advancedVisible" class="quote-panel">
      <div class="panel-heading">
        <div>
          <h2>{{ mode === 'fba' ? 'FBA 专线参数' : '快递小包参数' }}</h2>
          <span>带 <b>*</b> 的字段为必填项</span>
        </div>
        <el-button text :icon="Refresh" :loading="loadingProducts" @click="loadProducts">刷新渠道</el-button>
      </div>

      <el-form :model="form" label-position="top" class="quote-form" @submit.prevent="submitQuote">
        <div class="form-grid form-grid-main">
          <el-form-item label="销售渠道（可选）" class="channel-field">
            <el-select v-model="form.hubinCode" filterable clearable placeholder="不选则查询全部渠道" :loading="loadingProducts">
              <el-option v-for="product in products" :key="product.code" :label="productLabel(product)" :value="product.code">
                <span>{{ product.name || product.code }}</span><small>{{ product.code }}</small>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="目的国家/地区" required>
            <el-select v-model="form.destNo" filterable allow-create default-first-option placeholder="如 US">
              <el-option v-for="country in countries" :key="country.code" :label="country.label" :value="country.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="仓库代码">
            <el-input v-model="form.reWarehouseCode" placeholder="可选" clearable />
          </el-form-item>
          <el-form-item label="重量 (KG)" required>
            <el-input-number v-model="form.weig" :min="0.001" :precision="3" :step="0.001" :controls="false" />
          </el-form-item>
          <el-form-item label="包装类型" required>
            <el-select v-model="form.packing">
              <el-option label="WPX / 包裹" value="WPX" />
              <el-option label="DOC / 文件" value="DOC" />
              <el-option label="PAK / 袋" value="PAK" />
            </el-select>
          </el-form-item>
          <el-form-item label="收件邮编">
            <el-input v-model="form.reZip" placeholder="可选" clearable />
          </el-form-item>
        </div>

        <div class="subsection-title">单件尺寸 <span>单位：CM；直接修改体积也可覆盖自动计算</span></div>
        <div class="form-grid form-grid-size">
          <el-form-item label="长" :required="!hasManualVolume"><el-input-number v-model="form.length" :min="0.1" :precision="1" :step="0.1" :controls="false" @change="dimensionsChanged" /></el-form-item>
          <el-form-item label="宽" :required="!hasManualVolume"><el-input-number v-model="form.width" :min="0.1" :precision="1" :step="0.1" :controls="false" @change="dimensionsChanged" /></el-form-item>
          <el-form-item label="高" :required="!hasManualVolume"><el-input-number v-model="form.height" :min="0.1" :precision="1" :step="0.1" :controls="false" @change="dimensionsChanged" /></el-form-item>
          <el-form-item label="体积 (CM³)" required><el-input-number v-model="volumeInput" :min="0.001" :precision="3" :step="0.001" :controls="false" /></el-form-item>
        </div>

        <div v-if="mode === 'fba'" class="fba-extra">
          <div class="subsection-title">FBA 收件信息 <span>用于专线分区和末端报价</span></div>
          <div class="form-grid form-grid-fba">
            <el-form-item label="参考编号" required><el-input v-model="form.referenceno" placeholder="客户单号或 PO 号" /></el-form-item>
            <el-form-item label="收件州省" required><el-input v-model="form.reState" /></el-form-item>
            <el-form-item label="收件城市" required><el-input v-model="form.reCity" /></el-form-item>
            <el-form-item label="收件区县"><el-input v-model="form.reCounty" /></el-form-item>
            <el-form-item label="收件人" required><el-input v-model="form.reName" /></el-form-item>
            <el-form-item label="收件公司" required><el-input v-model="form.reCompany" /></el-form-item>
            <el-form-item label="收件电话" required><el-input v-model="form.reTel" /></el-form-item>
            <el-form-item label="收件地址 1" required><el-input v-model="form.reAddr" /></el-form-item>
          </div>
          <el-collapse class="sender-collapse">
            <el-collapse-item title="补充寄件信息（可选）" name="sender">
              <div class="form-grid form-grid-fba">
                <el-form-item label="寄件人"><el-input v-model="form.sdName" /></el-form-item>
                <el-form-item label="寄件公司"><el-input v-model="form.sdCompany" /></el-form-item>
                <el-form-item label="寄件电话"><el-input v-model="form.sdTel" /></el-form-item>
                <el-form-item label="寄件城市"><el-input v-model="form.sdCity" /></el-form-item>
                <el-form-item label="寄件地址"><el-input v-model="form.sdAddr" /></el-form-item>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div class="form-footer">
          <div class="quick-options">
            <el-checkbox-group v-model="form.goodsTypes">
              <el-checkbox label="PH">普货 PH</el-checkbox>
              <el-checkbox label="PI">带电 PI</el-checkbox>
            </el-checkbox-group>
            <el-select v-model="form.cocustomType" placeholder="报关方式" clearable>
              <el-option label="一般贸易" value="GENERAL" />
              <el-option label="无报关" value="NONE" />
            </el-select>
          </div>
          <el-button type="primary" size="large" :loading="loading" :icon="Search" native-type="submit">查询报价</el-button>
        </div>
      </el-form>
    </section>

    <section v-if="searched" class="results-section">
      <div class="results-toolbar">
        <div class="query-summary-block"><span>查询条件：</span><strong>{{ form.reWarehouseCode || '未指定仓库' }} / {{ form.destNo || '未选择目的地' }} / {{ goodsTypeLabel }} / 1件 / {{ form.weig }}KG / {{ volumeCbm }}CBM</strong><b>共{{ displayedResults.length }}条报价</b></div>
        <div class="toolbar-actions">
          <el-select v-model="resultFilter" size="small" style="width:150px" placeholder="全部渠道"><el-option label="全部渠道" value="" /><el-option v-for="item in carrierOptions" :key="item" :label="item" :value="item" /></el-select>
          <el-input v-model="resultKeyword" size="small" clearable placeholder="输入关键词筛选" style="width:190px" />
          <el-select v-model="sortBy" size="small" style="width:130px"><el-option label="价格排序" value="total" /><el-option label="时效优先" value="aging" /></el-select>
          <el-button-group><el-button size="small" :type="densityMode === 'comfortable' ? 'primary' : ''" @click="densityMode = 'comfortable'">宽松</el-button><el-button size="small" :type="densityMode === 'compact' ? 'primary' : ''" @click="densityMode = 'compact'">紧凑</el-button></el-button-group>
        </div>
      </div>
      <div class="result-columns"><span>销售产品</span><span>产品类别</span><span>参考时效</span><span>计费重</span><span>单价</span><span>预估总价</span><span>操作</span></div>
      <el-empty v-if="!displayedResults.length && !loading" description="暂无符合条件的渠道，请调整参数后重试" />
      <div v-else class="result-list" :class="`density-${densityMode}`">
        <article v-for="item in displayedResults" :key="item.id || item.code" class="result-card">
          <div class="result-brand"><div class="brand-mark" :class="carrierClass(item)">{{ carrierShort(item) }}</div><div><strong>{{ item.code || '未命名渠道' }}</strong><span>{{ item.name || '—' }}</span><small>材积除以数：{{ item.divsize ?? '—' }} · 公式：{{ item.formulaView || '—' }}</small></div></div>
          <div class="result-category">{{ item.hubType || carrierLabel(item) }}</div>
          <div class="result-aging">{{ item.aging || '未设置时效' }}</div>
          <div class="result-weight">{{ item.weig ?? form.weig }} {{ chargingLabel(item.chargingUnit) }}</div>
          <div class="result-unit">{{ money(item.otherFeeUnitSum || item.standardCharge, item.standardCur || item.currency) }}/{{ item.chargingUnit === 'CBM' ? 'CBM' : 'KG' }}</div>
          <div class="result-price"><strong>{{ money(item.standardFeeSum, item.standardCur || item.currency) }}</strong><el-button text type="primary" @click="showDetail(item)">明细</el-button></div>
          <div class="result-actions"><el-button type="primary" @click="startOrder(item)">立即下单</el-button></div>
        </article>
      </div>
    </section>

    <el-drawer v-model="detailVisible" title="报价费用明细" size="520px">
      <template v-if="activeResult">
        <div class="drawer-summary"><strong>{{ activeResult.name || activeResult.code }}</strong><span>{{ activeResult.code }}</span><b>{{ money(activeResult.standardFeeSum, activeResult.standardCur || activeResult.currency) }}</b></div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="运费">{{ money(activeResult.standardCharge, activeResult.standardCur || activeResult.currency) }}</el-descriptions-item>
          <el-descriptions-item label="附加费">{{ money(activeResult.otherStandardFeeSum, activeResult.standardCur || activeResult.currency) }}</el-descriptions-item>
          <el-descriptions-item label="计费重">{{ activeResult.weig ?? form.weig }} KG</el-descriptions-item>
          <el-descriptions-item label="分区">{{ activeResult.zoneName || activeResult.zoneNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="计算公式">{{ activeResult.formulaView || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h3 class="drawer-title">附加费列表</h3>
        <el-table :data="activeResult.otherStandardFeeList || []" size="small" border>
          <el-table-column prop="ardedName" label="费项" min-width="150" /><el-table-column label="金额" width="110"><template #default="{ row }">{{ money(row.chargestandard, row.chargeCur || activeResult.standardCur) }}</template></el-table-column><el-table-column prop="formula" label="公式" min-width="100" />
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Van, Box, Location, Refresh, Search } from '@element-plus/icons-vue'
import { expressQuoteApi } from '@/api'

defineOptions({ name: 'TianfuxingQuotes' })

const tabs = [
  { value: 'all', label: '全部产品', description: '快递小包 + FBA', icon: Connection },
  { value: 'small', label: '快递小包', description: '适用于包裹、文件、袋', icon: Box },
  { value: 'fba', label: 'FBA 专线', description: '需要完整收件信息', icon: Van }
]
const countries = `
AF 阿富汗|AX 奥兰群岛|AL 阿尔巴尼亚|DZ 阿尔及利亚|AS 美属萨摩亚|AD 安道尔|AO 安哥拉|AI 安圭拉|AQ 南极洲|AG 安提瓜和巴布达|AR 阿根廷|AM 亚美尼亚|AW 阿鲁巴|AU 澳大利亚|AT 奥地利|AZ 阿塞拜疆|BS 巴哈马|BH 巴林|BD 孟加拉国|BB 巴巴多斯|BY 白俄罗斯|BE 比利时|BZ 伯利兹|BJ 贝宁|BM 百慕大|BT 不丹|BO 玻利维亚|BQ 荷兰加勒比区|BA 波斯尼亚和黑塞哥维那|BW 博茨瓦纳|BV 布韦岛|BR 巴西|IO 英属印度洋领地|BN 文莱|BG 保加利亚|BF 布基纳法索|BI 布隆迪|CV 佛得角|KH 柬埔寨|CM 喀麦隆|CA 加拿大|KY 开曼群岛|CF 中非共和国|TD 乍得|CL 智利|CN 中国|CX 圣诞岛|CC 科科斯群岛|CO 哥伦比亚|KM 科摩罗|CG 刚果共和国|CD 刚果民主共和国|CK 库克群岛|CR 哥斯达黎加|CI 科特迪瓦|HR 克罗地亚|CU 古巴|CW 库拉索|CY 塞浦路斯|CZ 捷克|DK 丹麦|DJ 吉布提|DM 多米尼克|DO 多米尼加|EC 厄瓜多尔|EG 埃及|SV 萨尔瓦多|GQ 赤道几内亚|ER 厄立特里亚|EE 爱沙尼亚|SZ 埃斯瓦蒂尼|ET 埃塞俄比亚|FK 福克兰群岛|FO 法罗群岛|FJ 斐济|FI 芬兰|FR 法国|GF 法属圭亚那|PF 法属波利尼西亚|TF 法属南部领地|GA 加蓬|GM 冈比亚|GE 格鲁吉亚|DE 德国|GH 加纳|GI 直布罗陀|GR 希腊|GL 格陵兰|GD 格林纳达|GP 瓜德罗普|GU 关岛|GT 危地马拉|GG 根西岛|GN 几内亚|GW 几内亚比绍|GY 圭亚那|HT 海地|HM 赫德岛和麦克唐纳群岛|VA 梵蒂冈|HN 洪都拉斯|HK 中国香港|HU 匈牙利|IS 冰岛|IN 印度|ID 印度尼西亚|IR 伊朗|IQ 伊拉克|IE 爱尔兰|IM 马恩岛|IL 以色列|IT 意大利|JM 牙买加|JP 日本|JE 泽西岛|JO 约旦|KZ 哈萨克斯坦|KE 肯尼亚|KI 基里巴斯|KP 朝鲜|KR 韩国|KW 科威特|KG 吉尔吉斯斯坦|LA 老挝|LV 拉脱维亚|LB 黎巴嫩|LS 莱索托|LR 利比里亚|LY 利比亚|LI 列支敦士登|LT 立陶宛|LU 卢森堡|MO 中国澳门|MG 马达加斯加|MW 马拉维|MY 马来西亚|MV 马尔代夫|ML 马里|MT 马耳他|MH 马绍尔群岛|MQ 马提尼克|MR 毛里塔尼亚|MU 毛里求斯|YT 马约特|MX 墨西哥|FM 密克罗尼西亚|MD 摩尔多瓦|MC 摩纳哥|MN 蒙古|ME 黑山|MS 蒙特塞拉特|MA 摩洛哥|MZ 莫桑比克|MM 缅甸|NA 纳米比亚|NR 瑙鲁|NP 尼泊尔|NL 荷兰|NC 新喀里多尼亚|NZ 新西兰|NI 尼加拉瓜|NE 尼日尔|NG 尼日利亚|NU 纽埃|NF 诺福克岛|MK 北马其顿|MP 北马里亚纳群岛|NO 挪威|OM 阿曼|PK 巴基斯坦|PW 帕劳|PS 巴勒斯坦|PA 巴拿马|PG 巴布亚新几内亚|PY 巴拉圭|PE 秘鲁|PH 菲律宾|PN 皮特凯恩群岛|PL 波兰|PT 葡萄牙|PR 波多黎各|QA 卡塔尔|RE 留尼汪|RO 罗马尼亚|RU 俄罗斯|RW 卢旺达|BL 圣巴泰勒米|SH 圣赫勒拿|KN 圣基茨和尼维斯|LC 圣卢西亚|MF 法属圣马丁|PM 圣皮埃尔和密克隆|VC 圣文森特和格林纳丁斯|WS 萨摩亚|SM 圣马力诺|ST 圣多美和普林西比|SA 沙特阿拉伯|SN 塞内加尔|RS 塞尔维亚|SC 塞舌尔|SL 塞拉利昂|SG 新加坡|SX 荷属圣马丁|SK 斯洛伐克|SI 斯洛文尼亚|SB 所罗门群岛|SO 索马里|ZA 南非|GS 南乔治亚和南桑威奇群岛|SS 南苏丹|ES 西班牙|LK 斯里兰卡|SD 苏丹|SR 苏里南|SJ 斯瓦尔巴和扬马延|SE 瑞典|CH 瑞士|SY 叙利亚|TW 中国台湾|TJ 塔吉克斯坦|TZ 坦桑尼亚|TH 泰国|TL 东帝汶|TG 多哥|TK 托克劳|TO 汤加|TT 特立尼达和多巴哥|TN 突尼斯|TR 土耳其|TM 土库曼斯坦|TC 特克斯和凯科斯群岛|TV 图瓦卢|UG 乌干达|UA 乌克兰|AE 阿联酋|GB 英国|US 美国|UM 美国本土外小岛屿|UY 乌拉圭|UZ 乌兹别克斯坦|VU 瓦努阿图|VE 委内瑞拉|VN 越南|VG 英属维尔京群岛|VI 美属维尔京群岛|WF 瓦利斯和富图纳|EH 西撒哈拉|YE 也门|ZM 赞比亚|ZW 津巴布韦
`.trim().split('|').map(item => {
  const [code, ...name] = item.trim().split(' ')
  return { code, label: `${name.join(' ')} ${code}` }
})
const mode = ref('all')
const products = ref([])
const results = ref([])
const loading = ref(false)
const loadingProducts = ref(false)
const searched = ref(false)
const sortBy = ref('total')
const resultFilter = ref('')
const resultKeyword = ref('')
const densityMode = ref('comfortable')
const advancedVisible = ref(false)
const hasManualVolume = ref(false)
const manualVolumeValue = ref(null)
const detailVisible = ref(false)
const activeResult = ref(null)
const form = reactive({
  hubinCode: '', destNo: 'US', reWarehouseCode: '', weig: 1, packing: 'WPX', reZip: '', length: 30, width: 20, height: 15,
  goodsTypes: ['PH'], cocustomType: '', referenceno: `TFX-${Date.now()}`, density: '', issx: '', reState: '', reCity: '', reCounty: '', reName: '', reCompany: '', reTel: '', reAddr: '',
  sdName: '', sdCompany: '', sdTel: '', sdCity: '', sdAddr: ''
})
const calculatedVolume = computed(() => Math.round((form.length || 0) * (form.width || 0) * (form.height || 0) * 10) / 10)
const volumeInput = computed({
  get: () => hasManualVolume.value ? manualVolumeValue.value : calculatedVolume.value,
  set: value => {
    manualVolumeValue.value = value
    hasManualVolume.value = Number(value) > 0
  }
})
const activeVolume = computed(() => hasManualVolume.value ? Number(manualVolumeValue.value || 0) : calculatedVolume.value)
const volumeCbm = computed(() => (activeVolume.value / 1000000).toFixed(6))
const volumeCbmInput = computed({
  get: () => Number(volumeCbm.value),
  set: value => {
    const cbm = Number(value || 0)
    manualVolumeValue.value = cbm * 1000000
    hasManualVolume.value = cbm > 0
  }
})
const goodsTypeLabel = computed(() => {
  const labels = { PH: '普货', PI: '带电' }
  return form.goodsTypes?.length ? form.goodsTypes.map(type => labels[type] || type).join('、') : '普货'
})
const sortedResults = computed(() => [...results.value].sort((a, b) => sortBy.value === 'aging' ? String(a.aging || '').localeCompare(String(b.aging || '')) : Number(a.standardFeeSum || 0) - Number(b.standardFeeSum || 0)))
const carrierLabel = (item) => {
  if (item.hubType) return item.hubType
  const code = String(item.code || '')
  if (code.startsWith('DHK')) return 'DHL-HK'
  if (code.startsWith('FHK')) return 'FED-HK'
  if (code.startsWith('UHK')) return 'UPS-HK'
  return code.split(/[0-9]/)[0] || '其他'
}
const carrierShort = (item) => carrierLabel(item).replace(/-.*/, '').slice(0, 4) || 'TFX'
const carrierClass = (item) => carrierLabel(item).toLowerCase().replace(/[^a-z0-9]+/g, '-') || 'other'
const carrierOptions = computed(() => [...new Set(results.value.map(carrierLabel).filter(Boolean))])
const displayedResults = computed(() => {
  const keyword = resultKeyword.value.trim().toLowerCase()
  return sortedResults.value.filter(item => {
    const matchesFilter = !resultFilter.value || carrierLabel(item) === resultFilter.value
    const text = `${item.code || ''} ${item.name || ''} ${item.hubType || ''}`.toLowerCase()
    return matchesFilter && (!keyword || text.includes(keyword))
  })
})

const switchMode = async (next) => { mode.value = next; results.value = []; searched.value = false; resultFilter.value = ''; resultKeyword.value = ''; advancedVisible.value = next === 'fba'; await loadProducts() }
const dimensionsChanged = () => { hasManualVolume.value = false; manualVolumeValue.value = null }
const productLabel = (product) => product.name && product.name !== product.code ? `${product.name} · ${product.code}` : product.code
const loadProducts = async () => {
  loadingProducts.value = true
  try {
    const type = mode.value === 'small' ? '1' : mode.value === 'fba' ? '3' : ''
    const res = await expressQuoteApi.products(type)
    const body = res.data?.body
    products.value = Array.isArray(body) ? body : []
    if (form.hubinCode && !products.value.some(item => item.code === form.hubinCode)) form.hubinCode = ''
  } catch { products.value = [] } finally { loadingProducts.value = false }
}
const validate = () => {
  const required = mode.value === 'fba' ? [['destNo', '请选择目的国家'], ['referenceno', '请输入参考编号'], ['reState', '请输入收件州省'], ['reCity', '请输入收件城市'], ['reName', '请输入收件人'], ['reCompany', '请输入收件公司'], ['reTel', '请输入收件电话'], ['reAddr', '请输入收件地址']] : [['destNo', '请选择目的国家']]
  const missing = required.find(([key]) => !String(form[key] ?? '').trim())
  if (missing) { ElMessage.warning(missing[1]); return false }
  const dimensionsComplete = [form.length, form.width, form.height].every(value => Number(value) > 0)
  if (!form.weig || (!hasManualVolume.value && !dimensionsComplete) || (hasManualVolume.value && !(Number(manualVolumeValue.value) > 0))) { ElMessage.warning(hasManualVolume.value ? '请输入有效体积' : '请完善重量和尺寸'); return false }
  if (mode.value === 'fba' && !dimensionsComplete) { ElMessage.warning('FBA 专线必须填写长、宽、高'); return false }
  return true
}
const payload = () => {
  const dimensionsComplete = [form.length, form.width, form.height].every(value => Number(value) > 0)
  const base = { businessBigType: mode.value === 'fba' ? '3' : '1', ...(form.hubinCode ? { hubinCodes: [form.hubinCode] } : {}), destNo: form.destNo.trim().toUpperCase(), reWarehouseCode: form.reWarehouseCode, weig: form.weig, packing: form.packing, reZip: form.reZip, goodsTypes: form.goodsTypes, cocustomType: form.cocustomType, volumn: String(activeVolume.value) }
  if (dimensionsComplete && (!hasManualVolume.value || mode.value === 'fba')) base.pieces = [{ actual: form.weig, length: form.length, width: form.width, height: form.height }]
  if (mode.value === 'fba') return { ...base, referenceno: form.referenceno, density: form.density, issx: form.issx, reState: form.reState, reCity: form.reCity, reCounty: form.reCounty, reName: form.reName, reCompany: form.reCompany, reTel: form.reTel, reAddr: form.reAddr, sdName: form.sdName, sdCompany: form.sdCompany, sdTel: form.sdTel, sdCity: form.sdCity, sdAddr: form.sdAddr }
  return base
}
const submitQuote = async () => {
  if (!validate()) return
  loading.value = true; searched.value = true
  try {
    const res = await expressQuoteApi.query(payload())
    const body = res.data?.body
    results.value = Array.isArray(body) ? body : []
    if (!results.value.length) ElMessage.info('接口未返回符合条件的报价')
  } catch { results.value = [] } finally { loading.value = false }
}
const chargingLabel = (value) => value === 'CBM' ? '体积 CBM' : value === 'KG' ? '重量 KG' : value || '—'
const money = (value, currency = '') => value === undefined || value === null || value === '' ? '—' : `${currency || 'CNY'} ${Number(value).toFixed(2)}`
const showDetail = (item) => { activeResult.value = item; detailVisible.value = true }
const startOrder = (item) => ElMessage.info(`已选择 ${item.name || item.code}，下单流程将在订单模块继续`)

onMounted(loadProducts)
</script>

<style scoped>
.tfx-page { max-width: 1440px; margin: 0 auto; color: #1f2937; }
.hero-panel { display:flex; justify-content:space-between; align-items:center; padding:26px 32px; border-radius:12px; background:linear-gradient(120deg,#123d78,#1f6a9e); color:#fff; box-shadow:0 12px 30px #123d7822; }
.eyebrow { font-size:12px; letter-spacing:.12em; opacity:.72; text-transform:uppercase; }
.hero-copy h1 { margin:8px 0 6px; font-size:28px; font-weight:700; }
.hero-copy p { margin:0; color:#dbeafe; font-size:14px; }
.hero-status { display:flex; gap:8px; align-items:center; padding:9px 13px; border:1px solid #ffffff45; border-radius:20px; color:#d9f99d; font-size:13px; }
.product-tabs { display:grid; grid-template-columns:repeat(3,1fr); gap:12px; margin:18px 0; }
.product-tab { display:flex; gap:13px; align-items:center; padding:17px 20px; border:1px solid #e5e7eb; border-radius:10px; background:#fff; color:#52606d; cursor:pointer; text-align:left; transition:.2s; }
.product-tab:hover { border-color:#aac2de; }
.product-tab.active { border-color:#195b93; background:#f3f8fd; color:#154b7b; box-shadow:0 4px 12px #195b9315; }
.tab-icon { width:34px; height:34px; display:grid; place-items:center; border-radius:9px; background:#eef4fa; color:#2671a9; font-size:19px; }
.product-tab strong,.product-tab small { display:block; }.product-tab strong { font-size:15px; }.product-tab small { margin-top:4px; color:#8a98a8; font-size:12px; }
.quote-panel,.results-section { padding:24px 26px; border:1px solid #e9edf2; border-radius:12px; background:#fff; box-shadow:0 5px 18px #1f293708; }
.panel-heading,.results-toolbar { display:flex; justify-content:space-between; align-items:center; }.panel-heading { margin-bottom:20px; }.panel-heading h2,.results-toolbar h2 { margin:0 0 5px; font-size:19px; }.panel-heading span { font-size:12px; color:#98a2b3; }.panel-heading b { color:#d9485f; }
.quote-form :deep(.el-form-item) { margin-bottom:16px; }.quote-form :deep(.el-input-number),.quote-form :deep(.el-select) { width:100%; }
.form-grid { display:grid; gap:14px; }.form-grid-main { grid-template-columns:2fr 1.2fr 1.2fr 1fr 1.1fr 1.2fr; }.form-grid-size { grid-template-columns:repeat(3,1fr) 2fr; align-items:end; }.form-grid-fba { grid-template-columns:repeat(4,1fr); }
.channel-field :deep(.el-select-dropdown__item) { display:flex; justify-content:space-between; }.channel-field small { color:#98a2b3; }
.subsection-title { display:flex; gap:10px; align-items:center; margin:10px 0 14px; padding-top:14px; border-top:1px solid #edf0f3; color:#344054; font-size:14px; font-weight:600; }.subsection-title span { color:#98a2b3; font-size:12px; font-weight:400; }
.volume-readout { min-height:56px; padding:0 16px; display:flex; flex-direction:column; justify-content:center; border-radius:7px; background:#f6f8fa; }.volume-readout span { color:#98a2b3; font-size:12px; }.volume-readout strong { margin-top:3px; color:#195b93; }
.sender-collapse { margin:0 0 8px; }.sender-collapse :deep(.el-collapse-item__header) { height:36px; color:#53708d; font-size:13px; }.sender-collapse :deep(.el-collapse-item__wrap) { border:0; }
.form-footer { display:flex; justify-content:space-between; align-items:center; gap:20px; padding-top:14px; border-top:1px solid #edf0f3; }.quick-options { display:flex; align-items:center; gap:18px; }.quick-options :deep(.el-checkbox) { margin-right:12px; }.quick-options :deep(.el-select) { width:140px; }
.results-section { margin-top:18px; }.results-toolbar { margin-bottom:18px; }.results-toolbar h2 { display:inline-block; margin-right:10px; }.toolbar-actions { display:flex; gap:14px; align-items:center; }.query-summary { color:#8391a2; font-size:13px; }
.result-list { display:grid; gap:12px; }.result-card { display:grid; grid-template-columns:2.2fr 1.7fr 1.15fr auto; gap:20px; align-items:center; padding:20px; border:1px solid #e7ebf0; border-radius:10px; }.result-brand,.result-data,.result-actions { display:flex; align-items:center; }.result-brand { gap:12px; }.brand-mark { width:45px; height:45px; display:grid; place-items:center; border-radius:9px; background:#eaf3fb; color:#195b93; font-weight:700; font-size:12px; }.result-brand strong,.result-brand span,.result-data label,.result-price label,.result-price span { display:block; }.result-brand strong { font-size:16px; }.result-brand span { margin-top:4px; color:#98a2b3; font-size:12px; }.result-data { gap:24px; }.result-data label,.result-price label { margin-bottom:5px; color:#98a2b3; font-size:12px; }.result-data strong { font-size:14px; font-weight:500; }.result-price strong { color:#e45757; font-size:21px; }.result-price span { margin-top:4px; color:#98a2b3; font-size:11px; }.result-actions { justify-content:flex-end; gap:8px; white-space:nowrap; }
.drawer-summary { display:grid; grid-template-columns:1fr auto; gap:6px 12px; margin-bottom:20px; }.drawer-summary strong { font-size:18px; }.drawer-summary span { color:#98a2b3; font-size:12px; }.drawer-summary b { grid-column:1 / -1; color:#e45757; font-size:22px; }.drawer-title { margin:24px 0 10px; font-size:15px; }
@media (max-width:1100px) { .form-grid-main { grid-template-columns:repeat(3,1fr); }.result-card { grid-template-columns:1.7fr 1.4fr 1fr; }.result-actions { grid-column:1 / -1; justify-content:flex-start; } }
@media (max-width:700px) { .hero-panel { padding:20px; }.hero-copy h1 { font-size:23px; }.hero-status { display:none; }.product-tabs { grid-template-columns:1fr; }.quote-panel,.results-section { padding:18px 14px; }.form-grid-main,.form-grid-size,.form-grid-fba { grid-template-columns:1fr 1fr; }.form-footer,.quick-options { align-items:flex-start; flex-direction:column; }.form-footer .el-button { width:100%; }.result-card { grid-template-columns:1fr; gap:15px; }.result-data { justify-content:space-between; }.toolbar-actions { align-items:flex-end; flex-direction:column; gap:6px; }.query-summary { text-align:right; }.results-toolbar { align-items:flex-end; } }

/* 报价工作台布局：线路和货物摘要保持常驻，完整字段按需展开。 */
.tfx-page { max-width: none; padding: 0 8px 36px; background: #f4f6f8; }
.product-tabs { display: flex; gap: 0; margin: 0 0 12px; padding: 0 18px; border: 1px solid #e5e9ef; border-radius: 10px 10px 0 0; background: #fff; box-shadow: 0 3px 12px #1f29370b; }
.product-tab { position: relative; flex: 0 0 auto; gap: 8px; padding: 18px 30px 15px; border: 0; border-radius: 0; background: transparent; color: #667085; box-shadow: none; }
.product-tab::after { position: absolute; right: 22px; bottom: 0; left: 22px; height: 3px; background: transparent; content: ''; }
.product-tab.active { background: transparent; color: #0b367e; box-shadow: none; }
.product-tab.active::after { background: #0b367e; }
.product-tab strong { font-size: 17px; }.product-tab small { display: none; }.tab-icon { width: auto; height: auto; background: transparent; font-size: 17px; }
.quote-workbench { padding: 16px 24px 24px; border-radius: 0 0 12px 12px; background: #fff; box-shadow: 0 7px 20px #1f293715; }
.route-strip { display: grid; grid-template-columns: 1fr 1px 1fr; gap: 28px; align-items: center; padding: 18px 28px; border-radius: 12px; background: #f6f8fa; }
.route-point { display: flex; align-items: center; gap: 12px; min-width: 0; color: #8a929d; font-size: 15px; }.route-point .el-icon { color: #0b367e; font-size: 22px; }.route-point strong { overflow: hidden; color: #0b367e; font-size: 18px; text-overflow: ellipsis; white-space: nowrap; }.route-select { width: 180px; }.route-select :deep(.el-input__wrapper) { padding: 0; background: transparent; box-shadow: none; }.route-select :deep(.el-input__inner) { color: #0b367e; font-size: 18px; font-weight: 700; }.route-divider { width: 1px; height: 30px; background: #d9dee6; }
.quick-bar { display: grid; grid-template-columns: 1.4fr .75fr .9fr 1fr auto 1.15fr; gap: 12px; margin-top: 14px; }.quick-cell { min-height: 66px; display: flex; align-items: center; justify-content: center; gap: 8px; border-radius: 10px; background: #f6f8fa; color: #172b4d; font-size: 17px; }.quick-cell span { color: #667085; font-size: 13px; }.quick-cell strong { color: #0b367e; font-size: 20px; }.quick-number { width: 120px; }.quick-number :deep(.el-input__wrapper) { padding: 0 4px; background: transparent; box-shadow: none; }.quick-number :deep(.el-input__inner) { color: #0b367e; font-size: 20px; font-weight: 700; text-align: right; }.cargo-cell { justify-content: flex-start; padding: 0 22px; }.cargo-cell .el-icon { color: #0b367e; }.cargo-cell strong { font-size: 17px; }.unit-cell strong,.volume-cell strong { font-size: 19px; }.more-button,.quote-button { height: 66px; font-size: 17px; }.more-button { min-width: 110px; border-color: #0b367e; color: #0b367e; }.quote-button { min-width: 180px; background: #0b367e; border-color: #0b367e; }
.quote-panel { margin-top: 14px; }.panel-heading { margin-bottom: 18px; }.panel-heading h2 { color: #0b367e; }
.results-section { margin-top: 16px; padding: 0; border: 0; border-radius: 0; background: transparent; box-shadow: none; }.results-toolbar { gap: 16px; margin-bottom: 12px; padding: 0 22px; }.query-summary-block { flex: 1; min-width: 0; color: #667085; font-size: 15px; line-height: 1.6; }.query-summary-block strong { color: #475467; font-weight: 500; }.query-summary-block b { margin-left: 18px; color: #0b367e; font-weight: 600; }.toolbar-actions { flex-wrap: wrap; justify-content: flex-end; gap: 8px; }.result-columns { display: grid; grid-template-columns: 2.6fr 1fr 1fr 1fr 1.2fr 1.25fr .9fr; gap: 12px; align-items: center; margin-bottom: 10px; padding: 20px 24px; border-radius: 12px; background: #fff; color: #344054; font-size: 16px; font-weight: 600; text-align: center; box-shadow: 0 3px 12px #1f29370b; }.result-columns span:first-child { text-align: left; padding-left: 132px; }
.result-list { gap: 12px; }.result-card { display: grid; grid-template-columns: 2.6fr 1fr 1fr 1fr 1.2fr 1.25fr .9fr; gap: 12px; min-height: 168px; padding: 24px; border: 0; border-radius: 12px; background: #fff; box-shadow: 0 4px 14px #1f293712; }.result-list.density-compact .result-card { min-height: 120px; padding-top: 16px; padding-bottom: 16px; }.result-brand { min-width: 0; }.brand-mark { flex: 0 0 72px; width: 72px; height: 72px; border-radius: 50%; background: #f3f4f6; color: #0b367e; font-size: 14px; }.brand-mark.dhl-hk,.brand-mark.dhl { background: #ffcb05; color: #d40511; }.brand-mark.fed-hk,.brand-mark.fed { background: #f1f2f5; color: #4d148c; }.result-brand strong { color: #0b367e; font-size: 18px; }.result-brand span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.result-brand small { display: block; margin-top: 8px; color: #667085; font-size: 12px; line-height: 1.6; }.result-category,.result-aging,.result-weight,.result-unit { color: #667085; font-size: 15px; text-align: center; }.result-price { display: flex; align-items: center; justify-content: center; gap: 4px; }.result-price strong { color: #f04438; font-size: 21px; white-space: nowrap; }.result-price .el-button { padding: 0 2px; }.result-actions { justify-content: center; }.result-actions .el-button { min-width: 118px; background: #0b367e; border-color: #0b367e; }.drawer-summary b { color: #f04438; }
@media (max-width:1100px) { .quick-bar { grid-template-columns: repeat(3, 1fr); }.cargo-cell { grid-column: span 2; }.quote-button { min-width: 0; }.result-columns { display: none; }.result-card { grid-template-columns: 2fr 1fr 1fr; }.result-category,.result-aging,.result-weight,.result-unit { text-align: left; }.result-price,.result-actions { justify-content: flex-start; } }
@media (max-width:700px) { .tfx-page { padding: 0 0 24px; }.product-tabs { overflow-x: auto; padding: 0 6px; }.product-tab { padding: 14px 18px 12px; }.route-strip { grid-template-columns: 1fr; gap: 12px; padding: 14px 16px; }.route-divider { display: none; }.quick-bar { grid-template-columns: 1fr 1fr; }.cargo-cell { grid-column: span 2; }.quote-button { grid-column: span 2; }.results-toolbar { align-items: stretch; flex-direction: column; padding: 0 8px; }.toolbar-actions { justify-content: stretch; }.toolbar-actions > * { flex: 1; }.query-summary-block b { display: block; margin-left: 0; }.result-card { grid-template-columns: 1fr 1fr; padding: 18px; }.result-brand { grid-column: span 2; }.result-actions { grid-column: span 2; }.result-price { justify-content: flex-start; } }
</style>
