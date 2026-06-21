package com.banksystem.service;

import com.banksystem.dto.ClientRequestDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.Client;
import com.banksystem.model.Customer;
import com.banksystem.model.Merchant;
import com.banksystem.repository.ClientRepository;
import com.banksystem.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private ClientService clientService;

    private ClientRequestDTO customerRequest;
    private ClientRequestDTO merchantRequest;

    @BeforeEach
    void setUp() {
        customerRequest = new ClientRequestDTO();
        customerRequest.setClientType("CUSTOMER");
        customerRequest.setUcn("1234567890");
        customerRequest.setFirstName("Ivan");
        customerRequest.setLastName("Ivanov");

        merchantRequest = new ClientRequestDTO();
        merchantRequest.setClientType("MERCHANT");
        merchantRequest.setEik("123456789");
        merchantRequest.setCompanyName("Tech Ltd");
        merchantRequest.setRepresentativeFirstName("Jane");
        merchantRequest.setRepresentativeLastName("Smith");
    }

    @Test
    void addCustomer_success() {
        when(clientRepository.existsById("1234567890")).thenReturn(false);
        when(clientRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId("1234567890");
            return c;
        });

        Client result = clientService.addClient(customerRequest);

        assertThat(result).isInstanceOf(Customer.class);
        assertThat(result.getId()).isEqualTo("1234567890");
        verify(clientRepository).save(any(Customer.class));
    }

    @Test
    void addCustomer_duplicateUcn_throwsException() {
        when(clientRepository.existsById("1234567890")).thenReturn(true);

        assertThatThrownBy(() -> clientService.addClient(customerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Customer with this UCN already exists");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void addMerchant_success() {
        when(clientRepository.existsById("123456789")).thenReturn(false);
        when(clientRepository.save(any(Merchant.class))).thenAnswer(inv -> {
            Merchant m = inv.getArgument(0);
            m.setId("123456789");
            return m;
        });

        Client result = clientService.addClient(merchantRequest);

        assertThat(result).isInstanceOf(Merchant.class);
        assertThat(result.getId()).isEqualTo("123456789");
        verify(clientRepository).save(any(Merchant.class));
    }

    @Test
    void addMerchant_duplicateEik_throwsException() {
        when(clientRepository.existsById("123456789")).thenReturn(true);

        assertThatThrownBy(() -> clientService.addClient(merchantRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Merchant with this EIK already exists");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void addClient_invalidType_throwsException() {
        ClientRequestDTO invalid = new ClientRequestDTO();
        invalid.setClientType("UNKNOWN");

        assertThatThrownBy(() -> clientService.addClient(invalid))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid client type");
    }

    @Test
    void getClientById_found_returnsClient() {
        Customer customer = new Customer("1234567890", "Ivan", "Ivanov");
        when(clientRepository.findById("1234567890")).thenReturn(Optional.of(customer));

        Client result = clientService.getClientById("1234567890");
        assertThat(result).isEqualTo(customer);
    }

    @Test
    void getClientById_notFound_throwsException() {
        when(clientRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientById("unknown"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Client not found with id: unknown");
    }
}