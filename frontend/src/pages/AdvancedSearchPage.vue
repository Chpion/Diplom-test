<template>
  <q-page padding>
    <q-card class="q-pa-md">
      <q-card-section>
        <div class="text-h6">Расширенный поиск адреса</div>
      </q-card-section>
      <q-card-section>
        <q-form @submit="onSearch">
          <q-input
            v-model="region"
            :disable="loading"
            clearable
            :rules="[(val) => !!val || 'Регион обязателен']"
            label="Регион (например, обл. Ульяновская)"
            @input="onRegionInput"
          >
            <q-select
              v-if="regionResults.length"
              v-model="region"
              :options="regionResults"
              behavior="menu"
              use-input
              hide-selected
              fill-input
              @update:model-value="onRegionSelect"
            />
          </q-input>

          <q-input
            ref="cityInput"
            v-model="city"
            :disable="!isRegionSelectedFromList || loading"
            clearable
            :rules="[(val) => !!val || 'Город обязателен']"
            label="Город (например, г. Ульяновск)"
            @input="onCityInput"
          >
            <q-select
              v-if="cityResults.length"
              v-model="city"
              :options="cityResults"
              behavior="menu"
              use-input
              hide-selected
              fill-input
              @update:model-value="onCitySelect"
            />
          </q-input>

          <q-input
            v-model="street"
            :disable="!selectedCity || loading"
            clearable
            :rules="[(val) => !!val || 'Улица обязателен']"
            label="Улица (например, пр-кт. Созидателей)"
            @input="onStreetInput"
          >
            <q-select
              v-if="streetResults.length"
              v-model="street"
              :options="streetResults"
              behavior="menu"
              use-input
              hide-selected
              fill-input
              @update:model-value="onStreetSelect"
            />
          </q-input>

          <q-input
            v-model="house"
            :disable="!selectedStreet || loading"
            clearable
            label="Дом (например, ДОМ. 1)"
            @input="onHouseInput"
          >
            <q-select
              v-if="houseResults.length"
              v-model="house"
              :options="houseResults"
              behavior="menu"
              use-input
              hide-selected
              fill-input
              @update:model-value="onHouseSelect"
            />
          </q-input>

          <q-btn
            type="submit"
            color="primary"
            label="Поиск"
            class="q-mt-md q-mr-sm"
            :loading="loading"
            :disable="loading || !region"
          />
          <q-btn
            color="secondary"
            label="Получить результат"
            class="q-mt-md q-mr-sm"
            :loading="loadingDetails"
            :disable="!selectedRegion"
            @click="onGetDetails"
          />
          <q-btn
            color="negative"
            label="Сброс"
            class="q-mt-md"
            :disable="loading || loadingDetails"
            @click="resetForm"
          />
        </q-form>
      </q-card-section>
      <q-card-section v-if="addressInfo">
        <div class="text-subtitle1">Информация об адресе:</div>
        <q-list dense>
          <q-item>
            <q-item-section>
              <q-item-label>Полный адрес: {{ fullAddress }}</q-item-label>
              <q-item-label v-if="addressInfo.korp">Корпус: {{ addressInfo.korp }}</q-item-label>
              <q-item-label v-if="addressInfo.code">Код КЛАДР: {{ addressInfo.code }}</q-item-label>
              <q-item-label v-if="addressInfo.ocatd">Код ОКАТО: {{ addressInfo.ocatd }}</q-item-label>
              <q-item-label v-if="addressInfo.postalIndex">Почтовый индекс: {{ addressInfo.postalIndex }}</q-item-label>
              <q-item-label v-if="addressInfo.gninmb">ГНИМБ: {{ addressInfo.gninmb }}</q-item-label>
              <q-item-label v-if="addressInfo.uno">УНО: {{ addressInfo.uno }}</q-item-label>
              <q-item-label v-if="addressInfo.status !== null">Статус: {{ addressInfo.status }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { Notify } from 'quasar'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import api from 'src/services/api'

const store = useStore()
const router = useRouter()

const region = ref('')
const city = ref('')
const street = ref('')
const house = ref('')

const regionResults = ref([])
const cityResults = ref([])
const streetResults = ref([])
const houseResults = ref([])
const houseGroups = ref({}) // Для хранения групп домов

const selectedRegion = ref(null)
const selectedCity = ref(null)
const selectedStreet = ref(null)
const selectedHouse = ref(null)

const loading = ref(false)
const loadingDetails = ref(false)
const addressInfo = ref(null)

// Флаг для отслеживания выбора региона из выпадающего списка
const isRegionSelectedFromList = ref(false)

// Ссылка на поле "Город" для установки фокуса
const cityInput = ref(null)

// Вычисляемый полный адрес
const fullAddress = computed(() => {
  let address = ''
  if (selectedRegion.value) address += selectedRegion.value
  if (selectedCity.value) address += `, ${selectedCity.value}`
  if (selectedStreet.value) address += `, ${selectedStreet.value}`
  if (selectedHouse.value) {
    const houseNumber = selectedHouse.value.replace(/ДОМ\.\s*/i, '').trim()
    const matchingGroup = Object.keys(houseGroups.value).find(group =>
      houseGroups.value[group].includes(houseNumber)
    )
    if (matchingGroup) {
      address += `, ${matchingGroup}`
    } else {
      address += `, ${selectedHouse.value}`
    }
  }
  return address || 'Адрес не определен'
})

// Функции фильтрации по типам адресов
const filterRegions = (results) => {
  return results.filter(item => item.startsWith('обл.') || item.startsWith('р-н.'))
}

const filterCities = (results) => {
  return results.filter(item => item.startsWith('г.'))
}

const filterStreets = (results) => {
  return results.filter(item =>
    item.startsWith('ул.') ||
    item.startsWith('пер.') ||
    item.startsWith('мкр.') ||
    item.startsWith('кв-л.') ||
    item.startsWith('пр-кт.') ||
    item.startsWith('ш.') ||
    item.startsWith('б-р.') ||
    item.startsWith('аллея.')
  )
}

// Парсинг строки домов (например, "ДОМ. 1, 2, 3, 4, 10, 15")
const parseHouseGroup = (houseString) => {
  if (!houseString || !houseString.toLowerCase().startsWith('дом.')) return []
  const numbers = houseString
    .replace(/ДОМ\.\s*/i, '')
    .split(',')
    .map(num => num.trim())
    .filter(num => num)
  return numbers
}

// Универсальная функция для обработки ответа API
const processApiResponse = (data, context) => {
  const regions = filterRegions(data || [])
  const cities = filterCities(data || [])
  const streets = filterStreets(data || [])

  if (context === 'region') {
    regionResults.value = regions
    console.log('Processed regions:', regionResults.value)
    cityResults.value = []
    streetResults.value = []
    houseResults.value = []
    houseGroups.value = {}
  } else if (context === 'city') {
    cityResults.value = cities
    console.log('Processed cities:', cityResults.value)
    regionResults.value = []
    streetResults.value = []
    houseResults.value = []
    houseGroups.value = {}
  } else if (context === 'street') {
    streetResults.value = streets
    console.log('Processed streets:', streetResults.value)
    regionResults.value = []
    cityResults.value = []
    houseResults.value = []
    houseGroups.value = {}
  } else if (context === 'house') {
    // Парсим группы домов
    houseGroups.value = {}
    houseResults.value = []
    data.forEach(group => {
      const numbers = parseHouseGroup(group)
      if (numbers.length) {
        houseGroups.value[group] = numbers
        // Добавляем в выпадающий список только уникальные номера домов
        numbers.forEach(num => {
          if (!houseResults.value.includes(`ДОМ. ${num}`)) {
            houseResults.value.push(`ДОМ. ${num}`)
          }
        })
      }
    })
    console.log('Processed house groups:', houseGroups.value)
    console.log('House results for dropdown:', houseResults.value)
    regionResults.value = []
    cityResults.value = []
    streetResults.value = []
  }
}

// Метод для сброса всех данных
const resetForm = () => {
  console.log('Resetting form...')
  // Сбрасываем поля ввода
  region.value = ''
  city.value = ''
  street.value = ''
  house.value = ''

  // Сбрасываем выбранные значения
  selectedRegion.value = null
  selectedCity.value = null
  selectedStreet.value = null
  selectedHouse.value = null

  // Сбрасываем флаг региона
  isRegionSelectedFromList.value = false

  // Сбрасываем результаты поиска
  regionResults.value = []
  cityResults.value = []
  streetResults.value = []
  houseResults.value = []
  houseGroups.value = {}

  // Сбрасываем информацию об адресе
  addressInfo.value = null

  console.log('Form reset completed.')
}

const onRegionInput = async () => {
  if (region.value.length < 3) {
    regionResults.value = []
    return
  }
  loading.value = true
  try {
    const response = await api.advancedSearch({ region: region.value })
    processApiResponse(response.data, 'region')
  } catch (error) {
    Notify.create({
      type: 'negative',
      message: 'Поиск региона не удался: ' + (error.response?.data || 'Неизвестная ошибка'),
    })
    regionResults.value = []
  } finally {
    loading.value = false
  }
}

const onCityInput = async () => {
  if (!selectedRegion.value) {
    cityResults.value = []
    return
  }
  loading.value = true
  try {
    const params = { region: selectedRegion.value }
    if (city.value) params.city = city.value
    console.log('City input query:', params)
    const response = await api.advancedSearch(params)
    processApiResponse(response.data, 'city')
    if (cityResults.value.length && cityInput.value) {
      cityInput.value.focus()
    }
  } catch (error) {
    Notify.create({
      type: 'negative',
      message: 'Поиск города не удался: ' + (error.response?.data || 'Неизвестная ошибка'),
    })
    cityResults.value = []
  } finally {
    loading.value = false
  }
}

const onStreetInput = async () => {
  if (!selectedCity.value) {
    streetResults.value = []
    return
  }
  loading.value = true
  try {
    const params = { region: selectedRegion.value, city: selectedCity.value }
    if (street.value) params.street = street.value
    console.log('Street input query:', params)
    const response = await api.advancedSearch(params)
    processApiResponse(response.data, 'street')
  } catch (error) {
    Notify.create({
      type: 'negative',
      message: 'Поиск улицы не удался: ' + (error.response?.data || 'Неизвестная ошибка'),
    })
    streetResults.value = []
  } finally {
    loading.value = false
  }
}

const onHouseInput = async () => {
  console.log('onHouseInput triggered with house:', house.value, 'selectedStreet:', selectedStreet.value)
  if (!selectedStreet.value) {
    houseResults.value = []
    houseGroups.value = {}
    return
  }
  loading.value = true
  try {
    const params = {
      region: selectedRegion.value,
      city: selectedCity.value,
      street: selectedStreet.value,
    }
    if (house.value) params.house = house.value
    console.log('House input query:', params)
    const response = await api.advancedSearch(params)
    console.log('House API response:', response.data)
    processApiResponse(response.data, 'house')
  } catch (error) {
    Notify.create({
      type: 'negative',
      message: 'Поиск дома не удался: ' + (error.response?.data || 'Неизвестная ошибка'),
    })
    houseResults.value = []
    houseGroups.value = {}
  } finally {
    loading.value = false
  }
}

const onRegionSelect = async (value) => {
  console.log('onRegionSelect triggered with value:', value)
  selectedRegion.value = value
  isRegionSelectedFromList.value = true
  console.log('Selected region:', selectedRegion.value, 'isRegionSelectedFromList:', isRegionSelectedFromList.value)
  city.value = ''
  street.value = ''
  house.value = ''
  selectedCity.value = null
  selectedStreet.value = null
  selectedHouse.value = null
  cityResults.value = []
  streetResults.value = []
  houseResults.value = []
  houseGroups.value = {}
  addressInfo.value = null
  await nextTick()
  onCityInput()
}

const onCitySelect = (value) => {
  console.log('onCitySelect triggered with value:', value)
  selectedCity.value = value
  console.log('Selected city:', selectedCity.value)
  street.value = ''
  house.value = ''
  selectedStreet.value = null
  selectedHouse.value = null
  streetResults.value = []
  houseResults.value = []
  houseGroups.value = {}
  addressInfo.value = null
  onStreetInput()
}

const onStreetSelect = (value) => {
  console.log('onStreetSelect triggered with value:', value)
  selectedStreet.value = value
  console.log('Selected street:', selectedStreet.value)
  house.value = ''
  selectedHouse.value = null
  houseResults.value = []
  houseGroups.value = {}
  addressInfo.value = null
  onHouseInput()
}

const onHouseSelect = (value) => {
  console.log('onHouseSelect triggered with value:', value)
  selectedHouse.value = value
  console.log('Selected house:', selectedHouse.value)
  addressInfo.value = null
}

const onSearch = async () => {
  console.log('onSearch triggered', { region: region.value, city: city.value, street: street.value, house: house.value })
  if (!region.value) {
    Notify.create({
      type: 'negative',
      message: 'Заполните регион',
    })
    return
  }
  loading.value = true
  try {
    console.log('isRegionSelectedFromList:', isRegionSelectedFromList.value)
    if (!isRegionSelectedFromList.value) {
      console.log('Регион')
      await onRegionInput()
    } else if (isRegionSelectedFromList.value && !selectedCity.value) {
      console.log('Город')
      await onCityInput()
    } else if (selectedCity.value && !selectedStreet.value) {
      console.log('Улица')
      await onStreetInput()
    } else if (selectedStreet.value && !selectedHouse.value) {
      console.log('Дома')
      await onHouseInput()
    }
  } catch (error) {
    Notify.create({
      type: 'negative',
      message: 'Ошибка при поиске: ' + (error.response?.data || 'Неизвестная ошибка'),
    })
  } finally {
    loading.value = false
  }
}

const onGetDetails = async () => {
  if (!selectedRegion.value) {
    Notify.create({
      type: 'negative',
      message: 'Выберите регион для получения информации',
    })
    return
  }
  if (!store.getters['auth/isAuthenticated']) {
    Notify.create({
      type: 'negative',
      message: 'Требуется авторизация',
    })
    router.push('/login')
    return
  }
  loadingDetails.value = true
  try {
    // Формируем полный адрес
    let fullAddress = selectedRegion.value
    if (selectedCity.value) fullAddress += `, ${selectedCity.value}`
    if (selectedStreet.value) fullAddress += `, ${selectedStreet.value}`

    // Если выбран дом, добавляем его в запрос
    let houseForQuery = selectedHouse.value
    if (houseForQuery) {
      // Извлекаем номер дома (например, "1" из "ДОМ. 1")
      const houseNumber = houseForQuery.replace(/ДОМ\.\s*/i, '').trim()
      // Ищем группу, в которой есть этот номер
      const matchingGroup = Object.keys(houseGroups.value).find(group => {
        const numbers = houseGroups.value[group]
        return numbers.includes(houseNumber)
      })
      // Если группа найдена, используем её для запроса
      if (matchingGroup) {
        fullAddress += `, ${matchingGroup}`
      } else {
        fullAddress += `, ${houseForQuery}`
      }
    }

    const encodedKeyword = encodeURIComponent(fullAddress)
    console.debug(`Sending getAddressInfo for keyword: ${encodedKeyword}`)
    const { data } = await store.dispatch('auth/getAddressInfo', encodedKeyword)
    addressInfo.value = data
  } catch (error) {
    const message =
      error.response?.status === 400
        ? `Ошибка: ${error.response?.data?.error || 'Проверьте правильность введённого адреса'}`
        : `Не удалось получить информацию об адресе: ${error.response?.data?.error || error.message}`
    Notify.create({
      type: 'negative',
      message,
    })
    addressInfo.value = null
  } finally {
    loadingDetails.value = false
  }
}
</script>
