import isIP from 'validator/lib/isIP'
import isPort from 'validator/lib/isPort'

type ValidatorCallback = (error?: Error) => void
type ElementPlusValidator = (rule: unknown, value: unknown, callback: ValidatorCallback) => void

function createValidator(validate: (value: string) => boolean, message: string): ElementPlusValidator {
    return (_rule, value, callback) => {
        if (!validate(String(value ?? ''))) {
            callback(new Error(message))
            return
        }
        callback()
    }
}

export const Validator = {
    HostValidator(message: string): ElementPlusValidator {
        return createValidator((value) => isIP(value, 4), message)
    },
    PortValidator(message: string): ElementPlusValidator {
        return createValidator((value) => isPort(value), message)
    }
}
