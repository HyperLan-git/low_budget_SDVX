/*
* SDVX.c
*
* Created: 07/07/2023 22:23:44
* Author : HyperLan
* Helped myself with some code from https://github.com/ExploreEmbedded/ATmega32_ExploreUltraAvrDevKit/tree/master/Code/AVR%20Tutorial%20Code/00-libfiles
*/


#define F_CPU 16000000UL

#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include <time.h>
#include <stdbool.h>
#include <string.h>

#define PINC_OUT(x, b) if (b) { DDRC |= 1 << x; } else { PORTC &= ~(1 << x); }
#define PIND_OUT(x, b) if (b) { DDRD |= 1 << x; } else { PORTD &= ~(1 << x); }

#define PINB_ON(x) PORTB |= 1 << x
#define PINB_OFF(x) PORTB &= ~(1 << x)
#define PINC_ON(x) PORTC |= 1 << x
#define PINC_OFF(x) PORTC &= ~(1 << x)
#define PIND_ON(x) PORTD |= 1 << x
#define PIND_OFF(x) PORTD &= ~(1 << x)

#define PINC_READ(x) (PINC & (1 << x))
#define PIND_READ(x) (PIND & (1 << x))

#define M_GetBaudRateGeneratorValue(baudrate)  (((F_CPU -((baudrate) * 8L)) / ((baudrate) * 16UL)))

void Serial_Init(uint16_t baudRate) {
	UCSR0A = 0x00;								// Clear the UASRT status register
	UCSR0B = (1 << RXEN0) | (1 << TXEN0);		// Enable Receiver and Transmitter
	UCSR0C = (1 << UCSZ01) | (1 << UCSZ00);		// 8bits

	UBRR0 = M_GetBaudRateGeneratorValue(baudRate);
}

void Serial_writeChar(char c) {
	while ((UCSR0A & (1 << UDRE0)) == 0);		// Wait till Transmitter(UDR) register becomes Empty
	UDR0 = c;									// Load the data to be transmitted
}

void Serial_writeString(char* str) {
	while (*str) Serial_writeChar(*str++);
}

enum Rotation {
	NONE, LEFT, RIGHT, ERR
};

volatile int L = NONE, R = NONE;

ISR (PCINT2_vect) {
	bool LA = !PIND_READ(2), LB = !PIND_READ(3), RA = !PIND_READ(4), RB = !PIND_READ(5);

	if (L == NONE) {
		if (!LA && !LB) goto RIGHT;
		if (LA && !LB) {
			L = RIGHT;
			goto RIGHT;
		} else if(!LA && LB) {
			L = LEFT;
			goto RIGHT;
		}
		L = ERR;
	} else {
		if (!LA && !LB) L = NONE;
	}
	RIGHT:
	if (R == NONE) {
		if (!RA && !RB) return;
		if (RA && !RB) {
			R = RIGHT;
			return;
		} else if(!RA && RB) {
			R = LEFT;
			return;
		}
		R = ERR;
	} else {
		if (!RA && !RB) R = NONE;
	}
}

int main(void) {
	Serial_Init(9600);

	PORTD = 0b00111100;						// Enable PINS 2 3 4 5
	PCMSK2 = 0b00111100;					// Interrupt on pins 2 3 4 5
	PCICR = (1 << PCIE2);					// Enable pin change interrupt for port D

	sei();
	while (1) {
		if (R == LEFT) Serial_writeChar('q');
		else if (R == RIGHT) Serial_writeChar('d');
		if (L == LEFT) Serial_writeChar('k');
		else if (L == RIGHT) Serial_writeChar('m');
	}
	cli();
	return 0;
}

