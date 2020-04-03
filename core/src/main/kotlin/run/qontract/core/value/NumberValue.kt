package run.qontract.core.value

data class NumberValue(val number: Number) : Value {
    override val value: Any = number
    override val httpContentType = "text/plain"
    override fun toStringValue() = number.toString()
    override fun toString() = number.toString()
}