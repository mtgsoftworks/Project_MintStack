import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import {
    Table,
    TableHeader,
    TableBody,
    TableFooter,
    TableHead,
    TableRow,
    TableCell,
    TableCaption,
} from '../table'

describe('Table Components', () => {
    describe('Table', () => {
        it('renders children correctly', () => {
            render(
                <Table>
                    <tbody>
                        <tr>
                            <td>Content</td>
                        </tr>
                    </tbody>
                </Table>
            )
            expect(screen.getByRole('table')).toBeInTheDocument()
            expect(screen.getByText('Content')).toBeInTheDocument()
        })

        it('applies default classes', () => {
            render(
                <Table>
                    <tbody>
                        <tr>
                            <td>Test</td>
                        </tr>
                    </tbody>
                </Table>
            )
            expect(screen.getByRole('table')).toHaveClass('w-full', 'caption-bottom', 'text-sm')
        })

        it('wraps table in scrollable container', () => {
            render(
                <Table>
                    <tbody>
                        <tr>
                            <td>Test</td>
                        </tr>
                    </tbody>
                </Table>
            )
            const wrapper = screen.getByRole('table').parentElement
            expect(wrapper).toHaveClass('overflow-auto')
        })
    })

    describe('TableHeader', () => {
        it('renders as thead element', () => {
            render(
                <table>
                    <TableHeader>
                        <tr>
                            <th>Header</th>
                        </tr>
                    </TableHeader>
                </table>
            )
            expect(screen.getByRole('rowgroup')).toBeInTheDocument()
        })
    })

    describe('TableBody', () => {
        it('renders as tbody element', () => {
            render(
                <table>
                    <TableBody>
                        <tr>
                            <td>Body content</td>
                        </tr>
                    </TableBody>
                </table>
            )
            expect(screen.getByText('Body content')).toBeInTheDocument()
        })
    })

    describe('TableRow', () => {
        it('renders as tr element', () => {
            render(
                <table>
                    <tbody>
                        <TableRow>
                            <td>Row content</td>
                        </TableRow>
                    </tbody>
                </table>
            )
            expect(screen.getByRole('row')).toBeInTheDocument()
        })

        it('applies hover styling', () => {
            render(
                <table>
                    <tbody>
                        <TableRow>
                            <td>Content</td>
                        </TableRow>
                    </tbody>
                </table>
            )
            expect(screen.getByRole('row')).toHaveClass('hover:bg-muted/50')
        })
    })

    describe('TableHead', () => {
        it('renders as th element', () => {
            render(
                <table>
                    <thead>
                        <tr>
                            <TableHead>Column Header</TableHead>
                        </tr>
                    </thead>
                </table>
            )
            expect(screen.getByRole('columnheader')).toHaveTextContent('Column Header')
        })

        it('applies header styling', () => {
            render(
                <table>
                    <thead>
                        <tr>
                            <TableHead>Header</TableHead>
                        </tr>
                    </thead>
                </table>
            )
            expect(screen.getByRole('columnheader')).toHaveClass('font-medium', 'text-muted-foreground')
        })
    })

    describe('TableCell', () => {
        it('renders as td element', () => {
            render(
                <table>
                    <tbody>
                        <tr>
                            <TableCell>Cell Content</TableCell>
                        </tr>
                    </tbody>
                </table>
            )
            expect(screen.getByRole('cell')).toHaveTextContent('Cell Content')
        })

        it('applies cell padding', () => {
            render(
                <table>
                    <tbody>
                        <tr>
                            <TableCell>Padded Cell</TableCell>
                        </tr>
                    </tbody>
                </table>
            )
            expect(screen.getByRole('cell')).toHaveClass('p-4')
        })
    })

    describe('TableCaption', () => {
        it('renders caption text', () => {
            render(
                <table>
                    <TableCaption>Table description</TableCaption>
                    <tbody>
                        <tr>
                            <td>Data</td>
                        </tr>
                    </tbody>
                </table>
            )
            expect(screen.getByText('Table description')).toBeInTheDocument()
        })
    })

    describe('Full Table Composition', () => {
        it('renders complete table structure', () => {
            render(
                <Table>
                    <TableCaption>User list</TableCaption>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Name</TableHead>
                            <TableHead>Email</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        <TableRow>
                            <TableCell>John Doe</TableCell>
                            <TableCell>john@example.com</TableCell>
                        </TableRow>
                        <TableRow>
                            <TableCell>Jane Smith</TableCell>
                            <TableCell>jane@example.com</TableCell>
                        </TableRow>
                    </TableBody>
                    <TableFooter>
                        <TableRow>
                            <TableCell colSpan={2}>Total: 2 users</TableCell>
                        </TableRow>
                    </TableFooter>
                </Table>
            )

            expect(screen.getByRole('table')).toBeInTheDocument()
            expect(screen.getByText('User list')).toBeInTheDocument()
            expect(screen.getByText('Name')).toBeInTheDocument()
            expect(screen.getByText('Email')).toBeInTheDocument()
            expect(screen.getByText('John Doe')).toBeInTheDocument()
            expect(screen.getByText('jane@example.com')).toBeInTheDocument()
            expect(screen.getByText('Total: 2 users')).toBeInTheDocument()
        })
    })
})
